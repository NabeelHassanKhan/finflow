package com.finflow.service;

import com.finflow.dto.request.AccountRequest;
import com.finflow.dto.response.AccountResponse;
import com.finflow.entity.Account;
import com.finflow.entity.User;
import com.finflow.enums.AccountType;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.mapper.AccountMapper;
import com.finflow.repository.AccountRepository;
import com.finflow.repository.UserRepository;
import com.finflow.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountServiceImpl accountService;

    private User user;
    private final String userEmail = "sender@finflow.com";

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail(userEmail);
    }

    @Test
    void openAccount_withInitialDeposit_createsAccountSuccessfully() {
        // Arrange
        AccountRequest request = new AccountRequest();
        request.setAccountType(AccountType.SAVINGS);
        request.setInitialDeposit(new BigDecimal("1000.00"));

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));

        // generateUniqueAccountNumber() ke andar ye call hoti hai — hamesha false do
        // taake loop pehli hi baar mein exit ho jaye (koi collision nahi)
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);

        // save() jo bhi Account object mile usay wapis kar do (real DB simulate)
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse expectedResponse = new AccountResponse();
        when(accountMapper.toResponse(any(Account.class))).thenReturn(expectedResponse);

        // Act
        AccountResponse response = accountService.openAccount(userEmail, request);

        // Assert
        assertNotNull(response);

        // Assert: save() ko diya gaya Account object capture karke uske fields check karo
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());

        Account savedAccount = accountCaptor.getValue();
        assertEquals(new BigDecimal("1000.00"), savedAccount.getBalance());
        assertEquals(AccountType.SAVINGS, savedAccount.getAccountType());
        assertEquals(user, savedAccount.getUser());
        assertNotNull(savedAccount.getAccountNumber());
        assertTrue(savedAccount.getAccountNumber().startsWith("FIN"));
    }

    @Test
    void openAccount_withoutInitialDeposit_defaultsToZeroBalance() {
        // Arrange: initial deposit field bilkul set nahi kiya (null rahega)
        AccountRequest request = new AccountRequest();
        request.setAccountType(AccountType.CURRENT);
        // request.setInitialDeposit(...) — jaan-bujh kar call nahi kiya

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse expectedResponse = new AccountResponse();
        when(accountMapper.toResponse(any(Account.class))).thenReturn(expectedResponse);

        // Act
        accountService.openAccount(userEmail, request);

        // Assert: save() ko diya gaya object capture karke balance check karo
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());

        Account savedAccount = accountCaptor.getValue();
        assertEquals(BigDecimal.ZERO, savedAccount.getBalance());
    }

    @Test
    void openAccount_userNotFound_throwsResourceNotFoundException() {
        // Arrange: userRepository ko koi user na mile
        String unknownEmail = "ghost@finflow.com";
        AccountRequest request = new AccountRequest();
        request.setAccountType(AccountType.SAVINGS);
        request.setInitialDeposit(new BigDecimal("500.00"));

        when(userRepository.findByEmail(unknownEmail)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(ResourceNotFoundException.class, () ->
                accountService.openAccount(unknownEmail, request)
        );

        // Assert: user na milne ke baad account save NAHI hona chahiye
        verify(accountRepository, never()).save(any());
    }

    @Test
    void getBalance_accountExists_returnsCorrectBalance() {
        // Arrange
        Account account = new Account();
        account.setAccountNumber("ACC1001");
        account.setBalance(new BigDecimal("2500.50"));
        account.setUser(user);

        when(accountRepository.findByAccountNumberAndUserEmail("ACC1001", userEmail))
                .thenReturn(Optional.of(account));

        // Act
        BigDecimal balance = accountService.getBalance(userEmail, "ACC1001");

        // Assert: compareTo() use kiya, equals() nahi (BigDecimal scale-safety ke liye)
        assertEquals(0, new BigDecimal("2500.50").compareTo(balance));
    }

    @Test
    void getAccountDetails_accountNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(accountRepository.findByAccountNumberAndUserEmail("ACC9999", userEmail))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(ResourceNotFoundException.class, () ->
                accountService.getAccountDetails(userEmail, "ACC9999")
        );
    }
}