package com.enterprise.spendsync.core.internal.dto;

import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.domain.UserInvitation;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record SubAccountInvitationResponse(
        UUID id,
        String email,
        UUID targetLegalEntityId,
        String targetLegalEntityName,
        Set<RoleType> targetRoles,
        String inviteToken,
        String inviteUrl,
        boolean isAccepted,
        Instant expiresAt,
        Instant createdAt
) {
    public static SubAccountInvitationResponse fromEntity(UserInvitation invitation, String baseUrl) {
        String url = baseUrl + "/accept-invite?token=" + invitation.getInviteToken();
        return new SubAccountInvitationResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getTargetLegalEntity() != null ? invitation.getTargetLegalEntity().getId() : null,
                invitation.getTargetLegalEntity() != null ? invitation.getTargetLegalEntity().getName() : null,
                invitation.getTargetRoles(),
                invitation.getInviteToken(),
                url,
                invitation.isAccepted(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt()
        );
    }
}
