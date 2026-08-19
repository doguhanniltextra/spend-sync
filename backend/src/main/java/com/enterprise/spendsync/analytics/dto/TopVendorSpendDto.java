package com.enterprise.spendsync.analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TopVendorSpendDto(
        UUID vendorId,
        String vendorName,
        String taxNumber,
        String tier,
        BigDecimal volume,
        double sharePercent,
        String riskLevel
) {}
