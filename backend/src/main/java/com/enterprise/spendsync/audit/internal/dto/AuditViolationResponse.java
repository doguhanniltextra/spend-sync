package com.enterprise.spendsync.audit.internal.dto;

import com.enterprise.spendsync.audit.internal.domain.AuditAction;
import com.enterprise.spendsync.audit.internal.domain.AuditLog;
import com.enterprise.spendsync.audit.internal.domain.ComplianceTag;

import java.time.Instant;
import java.util.UUID;

public record AuditViolationResponse(
        UUID id,
        String correlationId,
        AuditAction action,
        ComplianceTag complianceTag,
        UUID actorId,
        String actorEmail,
        String actorRole,
        String ipAddress,
        String entityType,
        String entityId,
        String violationDetails,
        Instant timestamp
) {
    public static AuditViolationResponse from(AuditLog log) {
        return new AuditViolationResponse(
                log.getId(),
                log.getCorrelationId(),
                log.getAction(),
                log.getComplianceTag(),
                log.getActorId(),
                log.getActorEmail(),
                log.getActorRole(),
                log.getIpAddress(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDecisionNote(),
                log.getCreatedAt()
        );
    }
}
