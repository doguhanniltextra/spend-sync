package com.enterprise.spendsync.budget.internal.dto;

import com.enterprise.spendsync.budget.internal.domain.BudgetTransaction;
import com.enterprise.spendsync.budget.internal.domain.BudgetTransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BudgetTransactionResponse(
        UUID id,
        UUID budgetPoolId,
        BudgetTransactionType transactionType,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        UUID referenceId,
        String referenceType,
        String notes,
        Instant createdAt
) {
    public static BudgetTransactionResponse from(BudgetTransaction tx) {
        return new BudgetTransactionResponse(
                tx.getId(),
                tx.getBudgetPool().getId(),
                tx.getTransactionType(),
                tx.getAmount(),
                tx.getBalanceBefore(),
                tx.getBalanceAfter(),
                tx.getReferenceId(),
                tx.getReferenceType(),
                tx.getNotes(),
                tx.getCreatedAt()
        );
    }
}
