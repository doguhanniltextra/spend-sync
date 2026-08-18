package com.enterprise.spendsync.budget.internal.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetReservationResult(
        UUID budgetPoolId,
        boolean success,
        boolean isOverrun,
        String message,
        BigDecimal requestedAmount,
        BigDecimal availableBefore,
        BigDecimal availableAfter,
        BigDecimal reservedTotal
) {
    public static BudgetReservationResult success(UUID budgetPoolId,
                                                  boolean isOverrun,
                                                  String message,
                                                  BigDecimal requestedAmount,
                                                  BigDecimal availableBefore,
                                                  BigDecimal availableAfter,
                                                  BigDecimal reservedTotal) {
        return new BudgetReservationResult(
                budgetPoolId,
                true,
                isOverrun,
                message,
                requestedAmount,
                availableBefore,
                availableAfter,
                reservedTotal
        );
    }
}
