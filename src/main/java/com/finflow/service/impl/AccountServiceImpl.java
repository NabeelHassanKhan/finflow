package com.finflow.service.impl;

import com.finflow.dto.request.AccountRequest;
import com.finflow.dto.response.AccountResponse;
import com.finflow.entity.Account;
import com.finflow.entity.User;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.mapper.AccountMapper;
import com.finflow.repository.AccountRepository;
import com.finflow.repository.UserRepository;
import com.finflow.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional
    public AccountResponse openAccount(String username, AccountRequest request) {

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        BigDecimal openingBalance = request.getInitialDeposit() != null
                ? request.getInitialDeposit()
                : BigDecimal.ZERO;

        Account account = Account.builder()
                .accountNumber(generateUniqueAccountNumber())
                .accountType(request.getAccountType())
                .balance(openingBalance)
                .user(user)
                .build();

        Account savedAccount = accountRepository.save(account);

        return accountMapper.toResponse(savedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "accountDetails", key = "#email + '-' + #accountNumber")
    public AccountResponse getAccountDetails(String email,String accountNumber) {
        Account account = accountRepository.findByAccountNumberAndUserEmail(accountNumber , email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

        return accountMapper.toResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "accountBalance", key = "#email + '-' + #accountNumber")
    public BigDecimal getBalance(String email, String accountNumber) {
        Account account = accountRepository.findByAccountNumberAndUserEmail(accountNumber , email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

        return account.getBalance();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsForUser(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        return accountRepository.findByUserId(user.getId())
                .stream()
                .map(accountMapper::toResponse)
                .collect(Collectors.toList());
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            accountNumber = "FIN" + (100000000L + (long) (RANDOM.nextDouble() * 900000000L));
        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }
}