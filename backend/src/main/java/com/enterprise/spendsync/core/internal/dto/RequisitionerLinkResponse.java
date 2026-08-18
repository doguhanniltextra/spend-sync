package com.enterprise.spendsync.core.internal.dto;

import com.enterprise.spendsync.core.internal.domain.UserInvitation;

import java.time.Instant;
import java.util.UUID;

public record RequisitionerLinkResponse(
        UUID id,
        UUID targetLegalEntityId,
        String targetLegalEntityName,
        String inviteToken,
        String joinUrl,
        boolean isMultiUse,
        Instant expiresAt,
        Instant createdAt
) {
    public static RequisitionerLinkResponse fromEntity(UserInvitation invitation, String baseUrl) {
        String joinUrl = baseUrl + "/join/requisitioner?token=" + invitation.getInviteToken();
        return new RequisitionerLinkResponse(
                invitation.getId(),
                invitation.getTargetLegalEntity() != null ? invitation.getTargetLegalEntity().getId() : null,
                invitation.getTargetLegalEntity() != null ? invitation.getTargetLegalEntity().getName() : null,
                invitation.getInviteToken(),
                joinUrl,
                invitation.isMultiUse(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt()
        );
    }
}
