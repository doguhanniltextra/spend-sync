package com.enterprise.spendsync.intelligence.dto;

import jakarta.validation.constraints.NotBlank;

public record CopilotQueryRequest(
        @NotBlank(message = "query is required")
        String query,

        String contextId
) {}
