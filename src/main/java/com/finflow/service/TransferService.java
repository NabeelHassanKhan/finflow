package com.finflow.service;

import com.finflow.dto.request.TransferRequest;
import com.finflow.dto.response.TransactionResponse;

public interface TransferService {

    /**
     * Ek account se doosre account mein paisa transfer karta hai.
     *
     * @param userEmail logged-in user ki email (JWT se aayegi, request body se NAHI)
     * @param request   source, destination, amount, description
     * @return sender ki DEBIT transaction (naye balance ke saath)
     */

    TransactionResponse transfer(String userEmail, TransferRequest request);
    TransactionResponse executeTransfer(String userEmail, TransferRequest request);
    void evictBalance(String email, String accountNumber);
    void evictDetails(String email, String accountNumber);
}