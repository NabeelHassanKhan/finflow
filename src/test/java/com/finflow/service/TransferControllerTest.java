package com.finflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.controller.TransferController;
import com.finflow.dto.request.TransferRequest;
import com.finflow.dto.response.TransactionResponse;
import com.finflow.enums.TransactionStatus;
import com.finflow.enums.TransactionType;
import com.finflow.exception.ConcurrentTransferException;
import com.finflow.exception.DailyTransferLimitExceededException;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.security.CustomUserDetailsService;
import com.finflow.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
@WithMockUser(username = "test@example.com")
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Controller ki dependency: service mock hogi
    @MockitoBean
    private TransferService transferService;

    // JwtAuthFilter ki dependencies: context start hone ke liye zaroori
    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private static final String URL = "/api/transfers";

    private TransferRequest validRequest() {
        return TransferRequest.builder()
                .fromAccountNumber("ACC1001")
                .toAccountNumber("ACC2002")
                .amount(new BigDecimal("100.00"))
                .description("Rent")
                .build();
    }

    @Test
    @DisplayName("Valid transfer -> 201 CREATED + success ApiResponse")
    void transfer_validRequest_returns201() throws Exception {
        TransactionResponse response = TransactionResponse.builder()
                .transferReference("TRF-123")
                .accountNumber("ACC1001")
                .type(TransactionType.DEBIT)
                .status(TransactionStatus.SUCCESS)
                .amount(new BigDecimal("100.00"))
                .build();

        when(transferService.transfer(eq("test@example.com"), any(TransferRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post(URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Transfer completed successfully"))
                .andExpect(jsonPath("$.data.transferReference").value("TRF-123"));
    }

    @Test
    @DisplayName("Amount 0 -> 400 validation error, service call nahi hoti")
    void transfer_zeroAmount_returns400() throws Exception {
        TransferRequest request = validRequest();
        request.setAmount(BigDecimal.ZERO);

        mockMvc.perform(post(URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.amount").value("Amount must be at least 0.01"));

        verify(transferService, never()).transfer(any(), any());
    }

    @Test
    @DisplayName("ResourceNotFoundException -> 404")
    void transfer_accountNotFound_returns404() throws Exception {
        when(transferService.transfer(eq("test@example.com"), any(TransferRequest.class)))
                .thenThrow(new ResourceNotFoundException("Account not found"));

        mockMvc.perform(post(URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Account not found"));
    }

    @Test
    @DisplayName("DailyTransferLimitExceededException -> 422")
    void transfer_dailyLimitExceeded_returns422() throws Exception {
        when(transferService.transfer(eq("test@example.com"), any(TransferRequest.class)))
                .thenThrow(new DailyTransferLimitExceededException("Daily transfer limit exceeded"));

        mockMvc.perform(post(URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Daily transfer limit exceeded"));
    }

    @Test
    @DisplayName("ConcurrentTransferException -> 409")
    void transfer_concurrentConflict_returns409() throws Exception {
        when(transferService.transfer(eq("test@example.com"), any(TransferRequest.class)))
                .thenThrow(new ConcurrentTransferException("Transfer failed due to concurrent updates"));

        mockMvc.perform(post(URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Transfer failed due to concurrent updates"));
    }
}