package com.finflow.service;

import com.finflow.dto.request.TransactionFilterRequest;
import com.finflow.dto.response.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionService {

    Page<TransactionResponse> getTransactionHistory(
            String accountNumber,
            String userEmail,
            TransactionFilterRequest filterRequest,
            Pageable pageable
    );
}