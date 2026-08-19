package com.enterprise.spendsync.intelligence.dto;

import com.enterprise.spendsync.intelligence.domain.IntelligenceMode;
import com.enterprise.spendsync.intelligence.domain.TargetPersona;

import java.time.Instant;
import java.util.List;

public record ExecutivePulseResponse(
        TargetPersona persona,
        String personaTitle,
        String executiveSalutation,
        String executiveSummary,
        List<RecommendationCardDto> actionableRecommendations,
        FinancialPulseMetricsDto metrics,
        IntelligenceMode intelligenceMode,
        Instant evaluatedAt
) {}
