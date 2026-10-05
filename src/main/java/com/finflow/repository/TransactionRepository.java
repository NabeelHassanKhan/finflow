package com.finflow.repository;

import com.finflow.entity.Transaction;
import com.finflow.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> , JpaSpecificationExecutor<Transaction> {

    // Ek transfer ke dono legs (DEBIT + CREDIT) ek saath
    List<Transaction> findByTransferReference(String transferReference);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.account.accountNumber = :accountNumber
        AND t.type = :type
        AND t.status = com.finflow.enums.TransactionStatus.SUCCESS
        AND t.createdAt BETWEEN :startOfDay AND :endOfDay
    """)
    BigDecimal sumTransactionsByAccountAndTypeAndDateRange(
            @Param("accountNumber") String accountNumber,
            @Param("type") TransactionType type,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}