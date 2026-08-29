package com.enterprise.spendsync.notification.api.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain event dispatched when a budget pool's consumption surpasses critical thresholds (e.g. 80% or 95%).
 */
public record BudgetThresholdExceededEvent(
        UUID tenantId,
        UUID budgetPoolId,
        UUID costCenterId,
        String costCenterName,
        int fiscalYear,
        BigDecimal allocatedAmount,
        BigDecimal spentAmount,
        BigDecimal reservedAmount,
        double consumedPercentage,
        int thresholdPercentage,
        String currency
) {
    public static BudgetThresholdExceededEvent of(
            UUID tenantId,
            UUID budgetPoolId,
            UUID costCenterId,
            String costCenterName,
            int fiscalYear,
            BigDecimal allocatedAmount,
            BigDecimal spentAmount,
            BigDecimal reservedAmount,
            double consumedPercentage,
            int thresholdPercentage,
            String currency
    ) {
        return new BudgetThresholdExceededEvent(
                tenantId,
                budgetPoolId,
                costCenterId,
                costCenterName,
                fiscalYear,
                allocatedAmount,
                spentAmount,
                reservedAmount,
                consumedPercentage,
                thresholdPercentage,
                currency
        );
    }
}
