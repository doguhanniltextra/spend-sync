package com.enterprise.spendsync.audit.internal.web;

import com.enterprise.spendsync.audit.internal.domain.AuditAction;
import com.enterprise.spendsync.audit.internal.domain.ComplianceTag;
import com.enterprise.spendsync.audit.internal.dto.AuditLogResponse;
import com.enterprise.spendsync.audit.internal.dto.AuditTimelineItemResponse;
import com.enterprise.spendsync.audit.internal.dto.AuditViolationResponse;
import com.enterprise.spendsync.audit.internal.service.AuditService;
import com.enterprise.spendsync.shared.config.Endpoints;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping(Endpoints.Audit.BASE)
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping(Endpoints.Audit.LOGS)
    @PreAuthorize("hasAuthority('PERM_AUDIT_READ')")
    public ResponseEntity<List<AuditLogResponse>> getLogs(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) ComplianceTag tag,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        return ResponseEntity.ok(auditService.getLogs(action, tag, startDate, endDate));
    }

    @GetMapping(Endpoints.Audit.TIMELINE)
    @PreAuthorize("hasAuthority('PERM_AUDIT_READ')")
    public ResponseEntity<List<AuditTimelineItemResponse>> getEntityTimeline(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        return ResponseEntity.ok(auditService.getEntityTimeline(entityType, entityId));
    }

    @GetMapping(Endpoints.Audit.CORRELATION)
    @PreAuthorize("hasAuthority('PERM_AUDIT_READ')")
    public ResponseEntity<List<AuditLogResponse>> getByCorrelationId(@PathVariable String correlationId) {
        return ResponseEntity.ok(auditService.getByCorrelationId(correlationId));
    }

    @GetMapping(Endpoints.Audit.VIOLATIONS)
    @PreAuthorize("hasAuthority('PERM_AUDIT_READ')")
    public ResponseEntity<List<AuditViolationResponse>> getComplianceViolations() {
        return ResponseEntity.ok(auditService.getComplianceViolations());
    }
}
