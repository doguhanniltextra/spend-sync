package com.enterprise.spendsync.audit.internal.service;

import com.enterprise.spendsync.audit.internal.domain.AuditAction;
import com.enterprise.spendsync.audit.internal.domain.AuditLog;
import com.enterprise.spendsync.audit.internal.domain.ComplianceTag;
import com.enterprise.spendsync.audit.internal.dto.AuditLogResponse;
import com.enterprise.spendsync.audit.internal.dto.AuditTimelineItemResponse;
import com.enterprise.spendsync.audit.internal.dto.AuditViolationResponse;
import com.enterprise.spendsync.audit.internal.dto.RecordAuditRequest;
import com.enterprise.spendsync.audit.internal.repository.AuditLogRepository;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final TenantRepository tenantRepository;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(?i)(\"password\"\\s*:\\s*\")[^\"]+(\")");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(?i)(\"token\"\\s*:\\s*\")[^\"]+(\")");

    public AuditServiceImpl(AuditLogRepository auditLogRepository, TenantRepository tenantRepository) {
        this.auditLogRepository = auditLogRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLogResponse recordAuditLog(RecordAuditRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new SpendSyncException("Tenant not found", HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND") {});

        String maskedPayload = maskSensitiveData(request.payload());

        AuditLog log = new AuditLog(
                tenant,
                request.correlationId() != null ? request.correlationId() : UUID.randomUUID().toString(),
                request.action(),
                request.complianceTag() != null ? request.complianceTag() : ComplianceTag.ISO_27001_LOGGING,
                request.actorId(),
                request.actorEmail(),
                request.actorRole(),
                request.ipAddress(),
                request.userAgent(),
                request.entityType(),
                request.entityId(),
                request.legalEntityId(),
                request.costCenterId(),
                request.amount(),
                request.currency(),
                request.fromStatus(),
                request.toStatus(),
                request.decisionNote(),
                maskedPayload
        );

        AuditLog saved = auditLogRepository.save(log);
        return AuditLogResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getLogs(AuditAction action, ComplianceTag tag, Instant startDate, Instant endDate) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        List<AuditLog> logs;

        if (action != null) {
            logs = auditLogRepository.findAllByTenantIdAndActionOrderByCreatedAtDesc(tenantId, action);
        } else if (tag != null) {
            logs = auditLogRepository.findAllByTenantIdAndComplianceTagOrderByCreatedAtDesc(tenantId, tag);
        } else if (startDate != null && endDate != null) {
            logs = auditLogRepository.findAllByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, startDate, endDate);
        } else {
            logs = auditLogRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
        }

        return logs.stream().map(AuditLogResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditTimelineItemResponse> getEntityTimeline(String entityType, String entityId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return auditLogRepository.findAllByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtAsc(tenantId, entityType, entityId)
                .stream()
                .map(AuditTimelineItemResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getByCorrelationId(String correlationId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return auditLogRepository.findAllByTenantIdAndCorrelationIdOrderByCreatedAtAsc(tenantId, correlationId)
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditViolationResponse> getComplianceViolations() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return auditLogRepository.findAllByTenantIdAndComplianceTagOrderByCreatedAtDesc(tenantId, ComplianceTag.ISO_37001_SOD_CONTROL)
                .stream()
                .map(AuditViolationResponse::from)
                .toList();
    }

    private String maskSensitiveData(String payload) {
        if (payload == null || payload.isBlank()) {
            return payload;
        }
        String masked = PASSWORD_PATTERN.matcher(payload).replaceAll("$1********$2");
        return TOKEN_PATTERN.matcher(masked).replaceAll("$1********$2");
    }
}
