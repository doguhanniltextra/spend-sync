package com.enterprise.spendsync.intelligence.dto;

import java.math.BigDecimal;

public record FinancialPulseMetricsDto(
        BigDecimal totalAllocatedBudget,
        BigDecimal totalSpentMtd,
        BigDecimal totalCommittedMtd,
        BigDecimal budgetUtilizationPercent,
        BigDecimal upcomingDisbursement14Days,
        BigDecimal totalPotentialDiscountSavings,
        int pendingApprovalCount,
        BigDecimal pendingApprovalVolume,
        int criticalDiscrepancyCount,
        int budgetRunwayDaysLowest
) {}
