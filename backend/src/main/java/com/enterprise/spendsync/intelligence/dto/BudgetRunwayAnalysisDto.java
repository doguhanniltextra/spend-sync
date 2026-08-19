package com.enterprise.spendsync.intelligence.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BudgetRunwayAnalysisDto(
        UUID costCenterId,
        String costCenterCode,
        String costCenterName,
        BigDecimal allocatedAmount,
        BigDecimal spentAmount,
        BigDecimal reservedAmount,
        BigDecimal availableAmount,
        BigDecimal dailyBurnRate,
        int remainingRunwayDays,
        LocalDate estimatedExhaustionDate,
        boolean isExhaustionRisk
) {}
