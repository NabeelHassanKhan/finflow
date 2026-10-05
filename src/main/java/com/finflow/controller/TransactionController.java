package com.finflow.controller;

import com.finflow.dto.request.TransactionFilterRequest;
import com.finflow.dto.response.ApiResponse;
import com.finflow.dto.response.TransactionResponse;
import com.finflow.enums.TransactionType;
import com.finflow.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactionHistory(
            @PathVariable String accountNumber,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();

        TransactionFilterRequest filterRequest = TransactionFilterRequest.builder()
                .transactionType(type)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        Page<TransactionResponse> history = transactionService.getTransactionHistory(
                accountNumber, userEmail, filterRequest, pageable
        );

        return ResponseEntity.ok(ApiResponse.success("Transaction history fetched successfully", history));
    }
}