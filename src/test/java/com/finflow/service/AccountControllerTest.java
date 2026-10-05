package com.finflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.controller.AccountController;
import com.finflow.dto.request.AccountRequest;
import com.finflow.dto.response.AccountResponse;
import com.finflow.enums.AccountType;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.security.CustomUserDetailsService;
import com.finflow.security.JwtUtil;
import com.finflow.service.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@WithMockUser(username = "test@example.com")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    // JwtAuthFilter ki dependencies (context start hone ke liye)
    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private static final String BASE = "/api/accounts";
    private static final String EMAIL = "test@example.com";

    private AccountResponse sampleAccount() {
        return AccountResponse.builder()
                .id(1L)
                .accountNumber("ACC1001")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("500.50"))
                .ownerName("Test User")
                .build();
    }

    @Test
    @DisplayName("Open account valid -> 201 + success response")
    void openAccount_valid_returns201() throws Exception {
        AccountRequest request = AccountRequest.builder()
                .accountType(AccountType.SAVINGS)
                .initialDeposit(new BigDecimal("500.50"))
                .build();

        when(accountService.openAccount(eq(EMAIL), any(AccountRequest.class)))
                .thenReturn(sampleAccount());

        mockMvc.perform(post(BASE + "/open")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account opened successfully"))
                .andExpect(jsonPath("$.data.accountNumber").value("ACC1001"))
                .andExpect(jsonPath("$.data.accountType").value("SAVINGS"));
    }

    @Test
    @DisplayName("Open account, accountType missing -> 400")
    void openAccount_missingType_returns400() throws Exception {
        // Raw JSON: accountType hai hi nahi
        String json = "{\"initialDeposit\": 100}";

        mockMvc.perform(post(BASE + "/open")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.accountType").value("Account type is required"));

        verify(accountService, never()).openAccount(any(), any());
    }

    @Test
    @DisplayName("Open account, negative deposit -> 400")
    void openAccount_negativeDeposit_returns400() throws Exception {
        AccountRequest request = AccountRequest.builder()
                .accountType(AccountType.CURRENT)
                .initialDeposit(new BigDecimal("-10"))
                .build();

        mockMvc.perform(post(BASE + "/open")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.initialDeposit").value("Initial deposit cannot be negative"));

        verify(accountService, never()).openAccount(any(), any());
    }

    @Test
    @DisplayName("Get account details -> 200")
    void getAccountDetails_returns200() throws Exception {
        when(accountService.getAccountDetails(EMAIL, "ACC1001")).thenReturn(sampleAccount());

        mockMvc.perform(get(BASE + "/ACC1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account details fetched"))
                .andExpect(jsonPath("$.data.accountNumber").value("ACC1001"))
                .andExpect(jsonPath("$.data.ownerName").value("Test User"));
    }

    @Test
    @DisplayName("Get balance -> 200 + balance")
    void getBalance_returns200() throws Exception {
        when(accountService.getBalance(EMAIL, "ACC1001")).thenReturn(new BigDecimal("500.50"));

        mockMvc.perform(get(BASE + "/ACC1001/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Balance fetched"))
                .andExpect(jsonPath("$.data").value(500.50));
    }

    @Test
    @DisplayName("Get my accounts -> 200 + list (/my-accounts, {accountNumber} se capture nahi hota)")
    void getMyAccounts_returns200() throws Exception {
        when(accountService.getAccountsForUser(EMAIL)).thenReturn(List.of(sampleAccount()));

        mockMvc.perform(get(BASE + "/my-accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Accounts fetched"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].accountNumber").value("ACC1001"));
    }

    @Test
    @DisplayName("Account not found (ya kisi aur ka account) -> 404")
    void getAccountDetails_notFound_returns404() throws Exception {
        when(accountService.getAccountDetails(EMAIL, "ACC9999"))
                .thenThrow(new ResourceNotFoundException("Account not found"));

        mockMvc.perform(get(BASE + "/ACC9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Account not found"));
    }
}