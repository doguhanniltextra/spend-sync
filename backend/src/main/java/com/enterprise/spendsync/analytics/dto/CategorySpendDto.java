package com.enterprise.spendsync.analytics.dto;

import java.math.BigDecimal;

public record CategorySpendDto(
        String category,
        BigDecimal amount,
        double sharePercent
) {}
