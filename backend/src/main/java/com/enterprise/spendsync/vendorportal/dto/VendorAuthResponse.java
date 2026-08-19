package com.enterprise.spendsync.vendorportal.dto;

import java.util.Set;
import java.util.UUID;

public record VendorAuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UUID vendorUserId,
        UUID vendorId,
        UUID tenantId,
        String email,
        String fullName,
        String companyName,
        Set<String> roles
) {}
