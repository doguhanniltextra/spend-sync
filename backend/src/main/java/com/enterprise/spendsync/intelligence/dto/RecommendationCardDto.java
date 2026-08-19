package com.enterprise.spendsync.intelligence.dto;

import com.enterprise.spendsync.intelligence.domain.InsightSeverity;
import com.enterprise.spendsync.intelligence.domain.InsightType;

import java.math.BigDecimal;
import java.util.UUID;

public record RecommendationCardDto(
        UUID id,
        InsightType type,
        InsightSeverity severity,
        String title,
        String headline,
        String detailedInsight,
        String primaryActionLabel,
        String targetRoute,
        BigDecimal quantifiableValue,
        String quantifiableUnit,
        String policyReference
) {}
