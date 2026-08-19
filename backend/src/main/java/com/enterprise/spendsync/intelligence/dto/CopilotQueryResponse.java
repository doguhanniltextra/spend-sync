package com.enterprise.spendsync.intelligence.dto;

import com.enterprise.spendsync.intelligence.domain.IntelligenceMode;

import java.time.Instant;
import java.util.List;

public record CopilotQueryResponse(
        String answerMarkdown,
        List<String> citedDataSources,
        List<String> citedPolicyClauses,
        IntelligenceMode executionMode,
        Instant answeredAt
) {}
