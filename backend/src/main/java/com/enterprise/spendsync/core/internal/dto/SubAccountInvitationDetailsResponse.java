package com.enterprise.spendsync.core.internal.dto;

import com.enterprise.spendsync.core.internal.domain.RoleType;

import java.time.Instant;
import java.util.Set;

/**
 * Public response returned to the frontend when a user opens the invitation link.
 */
public record SubAccountInvitationDetailsResponse(
        String companyName,
        String legalEntityName,
        String email,
        Set<RoleType> targetRoles,
        boolean isValid,
        Instant expiresAt
) {}
