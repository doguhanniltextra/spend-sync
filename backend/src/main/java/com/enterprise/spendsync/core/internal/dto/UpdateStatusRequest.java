package com.enterprise.spendsync.core.internal.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull(message = "Status flag is required")
        Boolean isActive
) {}
