package com.enterprise.spendsync.budget.internal.dto;

import java.math.BigDecimal;
import java.util.List;

public record BudgetSummaryResponse(
        int fiscalYear,
        int totalPools,
        BigDecimal totalAllocated,
        BigDecimal totalReserved,
        BigDecimal totalSpent,
        BigDecimal totalAvailable,
        List<BudgetPoolResponse> pools
) {}
