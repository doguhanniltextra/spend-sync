package com.enterprise.spendsync.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record CfoExecutiveDeckResponse(
        BigDecimal totalSpendYtd,
        BigDecimal totalCommittedSpend,
        BigDecimal totalAllocatedBudget,
        double overallBudgetUtilizationPercent,
        String currency,
        List<CategorySpendDto> categoryDistribution,
        List<MonthlyOutflowDto> cashOutflowForecast,
        List<TopVendorSpendDto> topVendors,
        ThreeWayMatchIntegrityDto matchIntegrity
) {}
