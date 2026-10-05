package com.finflow.service.impl;

import com.finflow.config.TransferLimitProperties;
import com.finflow.dto.request.TransferRequest;
import com.finflow.dto.response.TransactionResponse;
import com.finflow.entity.Account;
import com.finflow.entity.Transaction;
import com.finflow.enums.TransactionStatus;
import com.finflow.enums.TransactionType;
import com.finflow.exception.*;
import com.finflow.mapper.TransactionMapper;
import com.finflow.repository.AccountRepository;
import com.finflow.repository.TransactionRepository;
import com.finflow.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final TransferLimitProperties transferLimitProperties;

    // Self-injection — @Lazy zaroori hai
    @Lazy
    private final TransferService self;

    // ===== NAYA: retry wrapper, NO @Transactional =====
    @Override
    public TransactionResponse transfer(String userEmail, TransferRequest request) {

        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return self.executeTransfer(userEmail, request);

            } catch (ObjectOptimisticLockingFailureException ex) {
                log.warn("Optimistic lock conflict on attempt {} for {} -> {}",
                        attempt, request.getFromAccountNumber(), request.getToAccountNumber());

                if (attempt >= MAX_RETRY_ATTEMPTS) {
                    throw new ConcurrentTransferException(
                            "Transfer could not be completed due to high concurrent activity. Please try again."
                    );
                }
            }
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "accountBalance", key = "#userEmail + '-' + #request.fromAccountNumber"),
            @CacheEvict(value = "accountDetails", key = "#userEmail + '-' + #request.fromAccountNumber"),
    })
    public TransactionResponse executeTransfer(String userEmail, TransferRequest request) {

        // 1. Same account check (DB ke pas jaane se pehle hi ruk jao)
        if (request.getFromAccountNumber().equals(request.getToAccountNumber())) {
            throw new InvalidTransactionException("Source and destination accounts cannot be the same");
        }

        // 2. Dono accounts load karo
        Account from = accountRepository.findByAccountNumberAndUserEmail(request.getFromAccountNumber() , userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));

        Account to = accountRepository.findByAccountNumber(request.getToAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

        // 3. Ownership check (IDOR fix): source account logged-in user ka hona chahiye
        if (!from.getUser().getEmail().equals(userEmail)) {
            log.warn("Unauthorized transfer attempt by {} on account {}", userEmail, from.getAccountNumber());
            throw new AccessDeniedException("You can only transfer from your own account");
        }

        // 4. Balance check (BigDecimal mein compareTo, equals nahi)
        BigDecimal amount = request.getAmount();
        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        validateDailyTransferLimit(from.getAccountNumber(), amount);

        // 5. Balances update karo
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        // 6. Dono legs ka common reference
        String transferReference = UUID.randomUUID().toString();
        String description = request.getDescription();

        Transaction debit = Transaction.builder()
                .transferReference(transferReference)
                .account(from)
                .type(TransactionType.DEBIT)
                .status(TransactionStatus.SUCCESS)
                .amount(amount)
                .balanceAfter(from.getBalance())
                .counterpartyAccountNumber(to.getAccountNumber())
                .description(description)
                .build();

        Transaction credit = Transaction.builder()
                .transferReference(transferReference)
                .account(to)
                .type(TransactionType.CREDIT)
                .status(TransactionStatus.SUCCESS)
                .amount(amount)
                .balanceAfter(to.getBalance())
                .counterpartyAccountNumber(from.getAccountNumber())
                .description(description)
                .build();

        transactionRepository.save(debit);
        transactionRepository.save(credit);

        evictAccountCaches(to.getUser().getEmail(), to.getAccountNumber());


        log.info("Transfer {} completed: {} -> {}, amount {}",
                transferReference, from.getAccountNumber(), to.getAccountNumber(), amount);

        // 7. Mapping @Transactional ke andar (lazy loading safe)
        return transactionMapper.toResponse(debit);
    }

    private void validateDailyTransferLimit(String accountNumber, BigDecimal transferAmount) {

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        BigDecimal alreadyTransferredToday = transactionRepository
                .sumTransactionsByAccountAndTypeAndDateRange(
                        accountNumber,
                        TransactionType.DEBIT,
                        startOfDay,
                        endOfDay
                );

        BigDecimal totalAfterThisTransfer = alreadyTransferredToday.add(transferAmount);
        BigDecimal dailyLimit = transferLimitProperties.getDailyLimit();

        if (totalAfterThisTransfer.compareTo(dailyLimit) > 0) {
            BigDecimal remainingLimit = dailyLimit.subtract(alreadyTransferredToday);
            if (remainingLimit.compareTo(BigDecimal.ZERO) < 0) {
                remainingLimit = BigDecimal.ZERO;
            }

            throw new DailyTransferLimitExceededException(
                    "Daily transfer limit exceeded. Limit: " + dailyLimit +
                            ", Already used today: " + alreadyTransferredToday +
                            ", Remaining limit: " + remainingLimit
            );
        }
    }

    private void evictAccountCaches(String email, String accountNumber) {
        self.evictBalance(email, accountNumber);
        self.evictDetails(email, accountNumber);
    }

    @Override
    @CacheEvict(value = "accountBalance", key = "#email + '-' + #accountNumber")
    public void evictBalance(String email, String accountNumber) {
        // Body khali — kaam sirf annotation karta hai
    }

    @Override
    @CacheEvict(value = "accountDetails", key = "#email + '-' + #accountNumber")
    public void evictDetails(String email, String accountNumber) {
        // Body khali — kaam sirf annotation karta hai
    }

}