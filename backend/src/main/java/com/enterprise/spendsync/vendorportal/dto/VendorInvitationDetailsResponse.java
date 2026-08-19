package com.enterprise.spendsync.vendorportal.dto;

import java.time.Instant;
import java.util.UUID;

public record VendorInvitationDetailsResponse(
        UUID id,
        String token,
        String email,
        String taxNumber,
        String companyName,
        String status,
        Instant expiresAt
) {}
