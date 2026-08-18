package com.enterprise.spendsync.core.internal.dto;

import java.util.Set;
import java.util.UUID;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        String tokenType,
        UUID userId,
        String email,
        String fullName,
        UUID tenantId,
        Set<String> roles
) {
    public static AuthTokenResponse of(String accessToken,
                                       String refreshToken,
                                       long expiresInSeconds,
                                       UUID userId,
                                       String email,
                                       String fullName,
                                       UUID tenantId,
                                       Set<String> roles) {
        return new AuthTokenResponse(
                accessToken,
                refreshToken,
                expiresInSeconds,
                "Bearer",
                userId,
                email,
                fullName,
                tenantId,
                roles
        );
    }
}
