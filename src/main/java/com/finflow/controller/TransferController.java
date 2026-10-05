package com.finflow.controller;

import com.finflow.dto.request.TransferRequest;
import com.finflow.dto.response.ApiResponse;
import com.finflow.dto.response.TransactionResponse;
import com.finflow.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Tag(name = "Fund Transfers", description = "Move money between accounts (ledger-style, atomic, with daily limit)")
public class TransferController {

    private final TransferService transferService;
    @Operation(
            summary = "Transfer money between accounts",
            description = "Debits the source account and credits the destination account in one "
                    + "database transaction. The source account must belong to the logged-in user."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Transfer completed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or insufficient balance"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found (or not owned by user)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Concurrent update conflict, retry later"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Daily transfer limit exceeded")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        TransactionResponse response = transferService.transfer(email, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transfer completed successfully", response));
    }
}