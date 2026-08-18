package com.enterprise.spendsync.core.internal.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GenerateRequisitionerLinkRequest(
        @NotNull(message = "Target legal entity ID is required")
        UUID targetLegalEntityId,

        Integer expirationDays // Defaults to 7 days (168 hours)
) {}
