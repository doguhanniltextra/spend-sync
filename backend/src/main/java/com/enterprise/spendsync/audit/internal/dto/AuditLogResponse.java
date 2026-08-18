package com.enterprise.spendsync.audit.internal.dto;

import com.enterprise.spendsync.audit.internal.domain.AuditAction;
import com.enterprise.spendsync.audit.internal.domain.AuditLog;
import com.enterprise.spendsync.audit.internal.domain.ComplianceTag;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
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
        UUID legalEntityId,
        UUID costCenterId,
        BigDecimal amount,
        String currency,
        String fromStatus,
        String toStatus,
        String decisionNote,
        String payload,
        String checksum,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
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
                log.getLegalEntityId(),
                log.getCostCenterId(),
                log.getAmount(),
                log.getCurrency(),
                log.getFromStatus(),
                log.getToStatus(),
                log.getDecisionNote(),
                log.getPayload(),
                log.getChecksum(),
                log.getCreatedAt()
        );
    }
}
