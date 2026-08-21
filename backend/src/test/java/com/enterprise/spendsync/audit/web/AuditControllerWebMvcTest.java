package com.enterprise.spendsync.audit.web;

import com.enterprise.spendsync.audit.internal.domain.AuditAction;
import com.enterprise.spendsync.audit.internal.domain.ComplianceTag;
import com.enterprise.spendsync.audit.internal.dto.AuditLogResponse;
import com.enterprise.spendsync.audit.internal.dto.AuditTimelineItemResponse;
import com.enterprise.spendsync.audit.internal.dto.AuditViolationResponse;
import com.enterprise.spendsync.audit.internal.service.AuditService;
import com.enterprise.spendsync.audit.internal.web.AuditController;
import com.enterprise.spendsync.shared.config.Endpoints;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditController REST Web API Slice Tests (ISO 27001 Audit Logs & Violations)")
class AuditControllerWebMvcTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuditController auditController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(auditController).build();
    }

    @Test
    @DisplayName("GET /api/v1/audit/logs - returns filtered audit logs")
    void shouldGetAuditLogs() throws Exception {
        AuditLogResponse log = new AuditLogResponse(
                UUID.randomUUID(), "corr-1", AuditAction.REQUISITION_APPROVED, ComplianceTag.ISO_9001_TRACEABILITY,
                UUID.randomUUID(), "cfo@spendsync.com", "CFO", "127.0.0.1",
                "PURCHASE_REQUISITION", "PR-2026-00001", UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("75000.00"), "TRY", "PENDING_APPROVAL", "APPROVED",
                "Requisition approved", null, "checksum-abc", Instant.now()
        );

        when(auditService.getLogs(any(), any(), any(), any())).thenReturn(List.of(log));

        mockMvc.perform(get(Endpoints.Audit.BASE + Endpoints.Audit.LOGS)
                        .param("action", "REQUISITION_APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("REQUISITION_APPROVED"))
                .andExpect(jsonPath("$[0].entityId").value("PR-2026-00001"));
    }

    @Test
    @DisplayName("GET /api/v1/audit/timeline/{entityType}/{entityId} - returns chronological timeline")
    void shouldGetEntityTimeline() throws Exception {
        AuditTimelineItemResponse item = new AuditTimelineItemResponse(
                UUID.randomUUID(), AuditAction.REQUISITION_APPROVED,
                "cfo@spendsync.com", "CFO", "PENDING_APPROVAL", "APPROVED",
                new BigDecimal("75000.00"), "TRY", "Approved by CFO", "checksum-abc", Instant.now()
        );

        when(auditService.getEntityTimeline("PURCHASE_REQUISITION", "PR-2026-00001")).thenReturn(List.of(item));

        mockMvc.perform(get(Endpoints.Audit.BASE + Endpoints.Audit.TIMELINE
                        .replace("{entityType}", "PURCHASE_REQUISITION")
                        .replace("{entityId}", "PR-2026-00001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actorEmail").value("cfo@spendsync.com"))
                .andExpect(jsonPath("$[0].toStatus").value("APPROVED"));
    }

    @Test
    @DisplayName("GET /api/v1/audit/violations - returns compliance violations")
    void shouldGetComplianceViolations() throws Exception {
        AuditViolationResponse violation = new AuditViolationResponse(
                UUID.randomUUID(), "corr-v1", AuditAction.SOD_VIOLATION_BLOCKED, ComplianceTag.ISO_37001_SOD_CONTROL,
                UUID.randomUUID(), "violator@test.com", "BUYER", "127.0.0.1", "PURCHASE_ORDER", "PO-001",
                "Self-approval violation blocked", Instant.now()
        );

        when(auditService.getComplianceViolations()).thenReturn(List.of(violation));

        mockMvc.perform(get(Endpoints.Audit.BASE + Endpoints.Audit.VIOLATIONS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].complianceTag").value("ISO_37001_SOD_CONTROL"));
    }
}
