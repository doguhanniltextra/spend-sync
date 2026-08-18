package com.enterprise.spendsync.budget.internal.dto;

import com.enterprise.spendsync.budget.internal.domain.BudgetEnforcementMode;
import com.enterprise.spendsync.budget.internal.domain.BudgetPeriodType;
import com.enterprise.spendsync.budget.internal.domain.BudgetPool;
import com.enterprise.spendsync.budget.internal.domain.BudgetStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BudgetPoolResponse(
        UUID id,
        UUID tenantId,
        UUID legalEntityId,
        String legalEntityName,
        UUID costCenterId,
        String costCenterName,
        String costCenterCode,
        int fiscalYear,
        BudgetPeriodType periodType,
        String periodValue,
        BudgetStatus status,
        BudgetEnforcementMode enforcementMode,
        BigDecimal tolerancePercentage,
        BigDecimal allocatedAmount,
        BigDecimal reservedAmount,
        BigDecimal spentAmount,
        BigDecimal availableAmount,
        BigDecimal maxAllowedAllocation,
        String currency,
        Instant createdAt,
        Instant updatedAt
) {
    public static BudgetPoolResponse from(BudgetPool pool) {
        return new BudgetPoolResponse(
                pool.getId(),
                pool.getTenant().getId(),
                pool.getLegalEntity().getId(),
                pool.getLegalEntity().getName(),
                pool.getCostCenter().getId(),
                pool.getCostCenter().getName(),
                pool.getCostCenter().getCode(),
                pool.getFiscalYear(),
                pool.getPeriodType(),
                pool.getPeriodValue(),
                pool.getStatus(),
                pool.getEnforcementMode(),
                pool.getTolerancePercentage(),
                pool.getAllocatedAmount(),
                pool.getReservedAmount(),
                pool.getSpentAmount(),
                pool.getAvailableAmount(),
                pool.getMaxAllowedAllocation(),
                pool.getCurrency(),
                pool.getCreatedAt(),
                pool.getUpdatedAt()
        );
    }
}
