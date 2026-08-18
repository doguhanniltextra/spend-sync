package com.enterprise.spendsync.audit.internal.dto;

import com.enterprise.spendsync.audit.internal.domain.AuditAction;
import com.enterprise.spendsync.audit.internal.domain.ComplianceTag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record RecordAuditRequest(
        String correlationId,

        @NotNull(message = "Action is mandatory")
        AuditAction action,

        ComplianceTag complianceTag,

        UUID actorId,
        String actorEmail,
        String actorRole,
        String ipAddress,
        String userAgent,

        @NotBlank(message = "Entity type is mandatory")
        String entityType,

        @NotBlank(message = "Entity ID is mandatory")
        String entityId,

        UUID legalEntityId,
        UUID costCenterId,
        BigDecimal amount,
        String currency,
        String fromStatus,
        String toStatus,
        String decisionNote,
        String payload
) {}
