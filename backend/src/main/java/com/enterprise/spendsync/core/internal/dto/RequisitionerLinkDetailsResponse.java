package com.enterprise.spendsync.core.internal.dto;

import java.time.Instant;

public record RequisitionerLinkDetailsResponse(
        String companyName,
        String legalEntityName,
        String targetRole,
        boolean isValid,
        Instant expiresAt
) {}
