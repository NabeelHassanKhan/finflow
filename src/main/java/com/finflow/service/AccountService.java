package com.finflow.service;

import com.finflow.dto.request.AccountRequest;
import com.finflow.dto.response.AccountResponse;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    AccountResponse openAccount(String username, AccountRequest request);

    AccountResponse getAccountDetails(String email , String accountNumber);

    BigDecimal getBalance(String email, String accountNumber);

    List<AccountResponse> getAccountsForUser(String email);
}