package com.finflow.repository.specification;

import com.finflow.entity.Account;
import com.finflow.entity.Transaction;
import com.finflow.enums.TransactionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionSpecification {

    // Private constructor — ye utility class hai, instantiate nahi honi chahiye
    private TransactionSpecification() {
    }

    public static Specification<Transaction> filterBy(
            Account account,
            TransactionType type,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Account filter — hamesha mandatory (security ke liye)
            predicates.add(criteriaBuilder.equal(root.get("account"), account));

            // Type filter — optional (agar user ne diya tabhi add hoga)
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("transactionType"), type));
            }

            // Date range filter — optional
            if (startDateTime != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDateTime));
            }
            if (endDateTime != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDateTime));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}