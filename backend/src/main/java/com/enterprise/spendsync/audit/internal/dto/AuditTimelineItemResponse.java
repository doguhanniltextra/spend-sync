package com.enterprise.spendsync.audit.internal.dto;

import com.enterprise.spendsync.audit.internal.domain.AuditAction;
import com.enterprise.spendsync.audit.internal.domain.AuditLog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AuditTimelineItemResponse(
        UUID logId,
        AuditAction action,
        String actorEmail,
        String actorRole,
        String fromStatus,
        String toStatus,
        BigDecimal amount,
        String currency,
        String decisionNote,
        String checksum,
        Instant timestamp
) {
    public static AuditTimelineItemResponse from(AuditLog log) {
        return new AuditTimelineItemResponse(
                log.getId(),
                log.getAction(),
                log.getActorEmail(),
                log.getActorRole(),
                log.getFromStatus(),
                log.getToStatus(),
                log.getAmount(),
                log.getCurrency(),
                log.getDecisionNote(),
                log.getChecksum(),
                log.getCreatedAt()
        );
    }
}
