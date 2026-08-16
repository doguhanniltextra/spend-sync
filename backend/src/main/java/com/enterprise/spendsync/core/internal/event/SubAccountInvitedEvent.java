package com.enterprise.spendsync.core.internal.event;

import com.enterprise.spendsync.core.internal.domain.RoleType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Domain Event published whenever a Sub-Account user is invited to a tenant.
 */
public record SubAccountInvitedEvent(
        UUID invitationId,
        UUID tenantId,
        String recipientEmail,
        String companyName,
        String legalEntityName,
        Set<RoleType> targetRoles,
        String inviteToken,
        String inviteUrl,
        Instant expiresAt
) {}
