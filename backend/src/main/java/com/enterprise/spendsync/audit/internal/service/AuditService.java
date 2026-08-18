package com.enterprise.spendsync.audit.internal.service;

import com.enterprise.spendsync.audit.internal.domain.AuditAction;
import com.enterprise.spendsync.audit.internal.domain.ComplianceTag;
import com.enterprise.spendsync.audit.internal.dto.AuditLogResponse;
import com.enterprise.spendsync.audit.internal.dto.AuditTimelineItemResponse;
import com.enterprise.spendsync.audit.internal.dto.AuditViolationResponse;
import com.enterprise.spendsync.audit.internal.dto.RecordAuditRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditService {

    AuditLogResponse recordAuditLog(RecordAuditRequest request);

    List<AuditLogResponse> getLogs(AuditAction action, ComplianceTag tag, Instant startDate, Instant endDate);

    List<AuditTimelineItemResponse> getEntityTimeline(String entityType, String entityId);

    List<AuditLogResponse> getByCorrelationId(String correlationId);

    List<AuditViolationResponse> getComplianceViolations();
}
