package com.finflow.service;

import com.finflow.dto.request.TransferRequest;
import com.finflow.dto.response.TransactionResponse;
import com.finflow.entity.Account;
import com.finflow.entity.User;
import com.finflow.enums.TransactionType;
import com.finflow.exception.ConcurrentTransferException;
import com.finflow.exception.DailyTransferLimitExceededException;
import com.finflow.exception.InsufficientBalanceException;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.mapper.TransactionMapper;
import com.finflow.repository.AccountRepository;
import com.finflow.repository.TransactionRepository;
import com.finflow.config.TransferLimitProperties;
import com.finflow.service.impl.TransferServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransferLimitProperties transferLimitProperties;

    @Mock
    private TransferService self; // @Lazy self-injection wala proxy, mock kar rahe hain

    @InjectMocks
    private TransferServiceImpl transferService;

    @Mock
    private TransactionMapper transactionMapper;

    private Account fromAccount;
    private Account toAccount;
    private TransferRequest request;
    private final String userEmail = "sender@finflow.com";

    @BeforeEach
    void setUp() {
        User sender = new User();
        sender.setEmail(userEmail);

        User receiver = new User();
        receiver.setEmail("receiver@finflow.com");

        fromAccount = new Account();
        fromAccount.setAccountNumber("ACC1001");
        fromAccount.setBalance(new BigDecimal("5000.00"));
        fromAccount.setUser(sender);
        fromAccount.setVersion(0L);

        toAccount = new Account();
        toAccount.setAccountNumber("ACC2002");
        toAccount.setBalance(new BigDecimal("1000.00"));
        toAccount.setUser(receiver);
        toAccount.setVersion(0L);

        request = new TransferRequest();
        request.setFromAccountNumber("ACC1001");
        request.setToAccountNumber("ACC2002");
        request.setAmount(new BigDecimal("500.00"));
    }

    @Test
    void executeTransfer_successfulTransfer_debitsAndCreditsCorrectly() {
        // Arrange: jab repository se account dhoonda jaye to hamara sample data return ho
        when(accountRepository.findByAccountNumberAndUserEmail("ACC1001" , userEmail)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumber("ACC2002")).thenReturn(Optional.of(toAccount));

        // Daily limit check pass ho jaye (abhi tak koi transfer nahi hua maan lo)
        when(transactionRepository.sumTransactionsByAccountAndTypeAndDateRange(
                eq("ACC1001"), eq(TransactionType.DEBIT), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(BigDecimal.ZERO);

        when(transferLimitProperties.getDailyLimit()).thenReturn(new BigDecimal("100000.00"));

        TransactionResponse expectedResponse = new TransactionResponse();
        when(transactionMapper.toResponse(any())).thenReturn(expectedResponse);

        // Act
        TransactionResponse response = transferService.executeTransfer(userEmail, request);

        // Assert: response null nahi aana chahiye
        assertNotNull(response);

        // Assert: balances correctly update hue
        assertEquals(new BigDecimal("4500.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("1500.00"), toAccount.getBalance());

        // Assert: transaction records (DEBIT + CREDIT rows) save hue
        verify(transactionRepository, times(2)).save(any());
    }

    @Test
    void executeTransfer_insufficientBalance_throwsExceptionAndDoesNotSave() {
        // Arrange: fromAccount ka balance kam kar dete hain transfer amount se
        fromAccount.setBalance(new BigDecimal("100.00")); // amount hai 500.00, balance sirf 100.00

        when(accountRepository.findByAccountNumberAndUserEmail("ACC1001" , userEmail)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumber("ACC2002")).thenReturn(Optional.of(toAccount));

        // Act + Assert: exception throw honi chahiye
        assertThrows(InsufficientBalanceException.class, () ->
                transferService.executeTransfer(userEmail, request)
        );

        // Assert: balance check fail hone ke baad koi save call NAHI honi chahiye
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void executeTransfer_accountNotOwnedByUser_throwsAccessDeniedException() {
        // Arrange: fromAccount ka owner "sender@finflow.com" hai (setUp mein set kiya),
        // lekin caller "hacker@finflow.com" hai — mismatch
       String otherUserEmail = "hacker@finflow.com";

        when(accountRepository.findByAccountNumberAndUserEmail("ACC1001", otherUserEmail)).thenReturn(Optional.of(fromAccount));

        // Act + Assert
        assertThrows(ResourceNotFoundException.class, () ->
                transferService.executeTransfer(otherUserEmail, request)
        );

        // Assert: ownership check fail hone ke baad koi save call NAHI honi chahiye
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void executeTransfer_dailyLimitExceeded_throwsException() {
        // Arrange: aaj already 99800 transfer ho chuka hai, naya request 500 ka hai
        // 99800 + 500 = 100300 > 100000 limit -> exceed
        when(accountRepository.findByAccountNumberAndUserEmail("ACC1001" , userEmail)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumber("ACC2002")).thenReturn(Optional.of(toAccount));

        when(transactionRepository.sumTransactionsByAccountAndTypeAndDateRange(
                eq("ACC1001"),
                eq(TransactionType.DEBIT),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(new BigDecimal("99800.00"));

        when(transferLimitProperties.getDailyLimit()).thenReturn(new BigDecimal("100000.00"));

        // Act + Assert
        assertThrows(DailyTransferLimitExceededException.class, () ->
                transferService.executeTransfer(userEmail, request)
        );

        // Assert: limit check fail hone ke baad koi save call NAHI honi chahiye
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_firstAttemptLockConflict_secondAttemptSucceeds() {
        TransactionResponse expectedResponse = new TransactionResponse(); // ya jo bhi fields hain, khaali bhi chalega

        // Arrange: pehli call exception de, doosri call success de
        when(self.executeTransfer(userEmail, request))
                .thenThrow(new ObjectOptimisticLockingFailureException(Account.class, "ACC1001"))
                .thenReturn(expectedResponse);

        // Act
        TransactionResponse result = transferService.transfer(userEmail, request);

        // Assert
        assertSame(expectedResponse, result);

        // Assert: self.executeTransfer() exactly 2 baar call hua (1 fail + 1 success)
        verify(self, times(2)).executeTransfer(userEmail, request);
    }

    @Test
    void transfer_allAttemptsFail_throwsConcurrentTransferException() {
        // Arrange: har attempt pe lock conflict throw ho
        when(self.executeTransfer(userEmail, request))
                .thenThrow(new ObjectOptimisticLockingFailureException(Account.class, "ACC1001"));

        // Act + Assert
        assertThrows(ConcurrentTransferException.class, () ->
                transferService.transfer(userEmail, request)
        );

        // Assert: exactly MAX_RETRY_ATTEMPTS dafa try hua, na kam na zyada
        verify(self, times(3)).executeTransfer(userEmail, request); // agar MAX_RETRY_ATTEMPTS = 3
    }
}