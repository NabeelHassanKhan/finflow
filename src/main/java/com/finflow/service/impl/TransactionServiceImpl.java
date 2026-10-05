package com.finflow.service.impl;

import com.finflow.dto.request.TransactionFilterRequest;
import com.finflow.dto.response.TransactionResponse;
import com.finflow.entity.Account;
import com.finflow.entity.Transaction;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.mapper.TransactionMapper;
import com.finflow.repository.AccountRepository;
import com.finflow.repository.TransactionRepository;
import com.finflow.repository.specification.TransactionSpecification;
import com.finflow.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Override
    public Page<TransactionResponse> getTransactionHistory(
            String accountNumber,
            String userEmail,
            TransactionFilterRequest filterRequest,
            Pageable pageable
    ) {
        // Step 1: Ownership check — same IDOR-safe pattern jo Day 3/4 mein use kiya tha
        Account account = accountRepository.findByAccountNumberAndUserEmail(accountNumber, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found or does not belong to current user: " + accountNumber));

        // Step 2: LocalDate → LocalDateTime conversion (inclusive end date ke liye)
        LocalDateTime startDateTime = filterRequest.getStartDate() != null
                ? filterRequest.getStartDate().atStartOfDay()
                : null;

        LocalDateTime endDateTime = filterRequest.getEndDate() != null
                ? filterRequest.getEndDate().atTime(LocalTime.MAX) // 23:59:59.999999999
                : null;

        // Step 3: Dynamic Specification build karo
        Specification<Transaction> spec = TransactionSpecification.filterBy(
                account,
                filterRequest.getTransactionType(),
                startDateTime,
                endDateTime
        );

        // Step 4: Paginated, filtered query — actual DB call yahin hoti hai
        Page<Transaction> transactionPage = transactionRepository.findAll(spec, pageable);

        // Step 5: Entity Page -> DTO Page (Page.map() built-in method hai)
        return transactionPage.map(transactionMapper::toResponse);
    }
}