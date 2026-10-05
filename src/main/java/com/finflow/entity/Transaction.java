package com.finflow.entity;

import com.finflow.enums.TransactionStatus;
import com.finflow.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_txn_account_created", columnList = "account_id, created_at"),
                @Index(name = "idx_txn_transfer_ref", columnList = "transfer_reference")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Transfer ke dono legs (DEBIT + CREDIT) ka common reference
    @Column(name = "transfer_reference", nullable = false, length = 36)
    private String transferReference;

    // Ye row kis account ki history ka hissa hai
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    // Is transaction ke BAAD account ka balance (statement ka running balance)
    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    // Samne wali party ka account number
    @Column(name = "counterparty_account_number", length = 30)
    private String counterpartyAccountNumber;

    @Column(length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}