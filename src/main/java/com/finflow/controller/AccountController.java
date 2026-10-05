package com.finflow.controller;

import com.finflow.dto.request.AccountRequest;
import com.finflow.dto.response.AccountResponse;
import com.finflow.dto.response.ApiResponse;
import com.finflow.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Open accounts, view details and check balance")
public class AccountController {

    private final AccountService accountService;
    @Operation(
            summary = "Open a new account",
            description = "Opens a SAVINGS or CURRENT account for the logged-in user. "
                    + "Initial deposit is optional (defaults to 0)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Account opened"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed (missing type or negative deposit)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/open")
    public ResponseEntity<ApiResponse<AccountResponse>> openAccount(
            @Valid @RequestBody AccountRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        AccountResponse response = accountService.openAccount(email, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account opened successfully", response));
    }

    @Operation(
            summary = "Get account details",
            description = "Returns details of one account. Only the owner can view it "
                    + "(someone else's account number returns 404)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account details returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found (or not owned by user)")
    })
    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountDetails(Authentication authentication,
            @PathVariable String accountNumber) {
        String email = authentication.getName();

        AccountResponse response = accountService.getAccountDetails(email , accountNumber);

        return ResponseEntity.ok(ApiResponse.success("Account details fetched", response));
    }

    @Operation(
            summary = "Get account balance",
            description = "Returns the current balance of one account (cached in Redis). "
                    + "Only the owner can view it."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Balance returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found (or not owned by user)")
    })

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<ApiResponse<BigDecimal>> getBalance( Authentication authentication ,
            @PathVariable String accountNumber) {
        String email = authentication.getName();

        BigDecimal balance = accountService.getBalance(email , accountNumber);

        return ResponseEntity.ok(ApiResponse.success("Balance fetched", balance));
    }

    @Operation(
            summary = "List my accounts",
            description = "Returns all accounts that belong to the logged-in user."
    )
    @GetMapping("/my-accounts")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getMyAccounts(
            Authentication authentication) {

        String email = authentication.getName();
        List<AccountResponse> accounts = accountService.getAccountsForUser(email);

        return ResponseEntity.ok(ApiResponse.success("Accounts fetched", accounts));
    }
}