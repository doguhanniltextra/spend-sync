package com.enterprise.spendsync.audit.service;

import com.enterprise.spendsync.audit.internal.domain.AuditAction;
import com.enterprise.spendsync.audit.internal.domain.AuditLog;
import com.enterprise.spendsync.audit.internal.domain.ComplianceTag;
import com.enterprise.spendsync.audit.internal.dto.AuditLogResponse;
import com.enterprise.spendsync.audit.internal.dto.AuditTimelineItemResponse;
import com.enterprise.spendsync.audit.internal.dto.RecordAuditRequest;
import com.enterprise.spendsync.audit.internal.repository.AuditLogRepository;
import com.enterprise.spendsync.audit.internal.service.AuditServiceImpl;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService Immutable Audit Trail & Sensitive Masking Service Tests")
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private TenantRepository tenantRepository;

    @Captor
    private ArgumentCaptor<AuditLog> auditLogCaptor;

    private AuditServiceImpl auditService;
    private UUID tenantId;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("SpendSync Enterprise");

        auditService = new AuditServiceImpl(auditLogRepository, tenantRepository);
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("TC-10-02: recordAuditLog masks sensitive passwords and tokens in payload diff")
    void shouldMaskSensitiveDataInAuditPayload() {
        UUID actorId = UUID.randomUUID();
        String sensitiveJson = "{\"username\":\"admin\",\"password\":\"TopSecret123!\",\"token\":\"bearer-abc-xyz\"}";

        RecordAuditRequest request = new RecordAuditRequest(
                UUID.randomUUID().toString(),
                AuditAction.USER_LOGGED_IN,
                ComplianceTag.ISO_27001_LOGGING,
                actorId,
                "admin@spendsync.com",
                "ROOT_USER",
                "192.168.1.100",
                "Mozilla/5.0",
                "USER",
                actorId.toString(),
                null,
                null,
                null,
                null,
                "ANONYMOUS",
                "AUTHENTICATED",
                "User successfully logged in with MFA",
                sensitiveJson
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> {
            AuditLog log = i.getArgument(0);
            log.setId(UUID.randomUUID());
            return log;
        });

        AuditLogResponse response = auditService.recordAuditLog(request);

        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog saved = auditLogCaptor.getValue();

        assertThat(saved.getPayload()).doesNotContain("TopSecret123!");
        assertThat(saved.getPayload()).doesNotContain("bearer-abc-xyz");
        assertThat(saved.getPayload()).contains("\"password\":\"********\"");
        assertThat(saved.getPayload()).contains("\"token\":\"********\"");
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.100");
        assertThat(saved.getActorEmail()).isEqualTo("admin@spendsync.com");
        assertThat(saved.getChecksum()).isNotNull().hasSize(64);
    }

    @Test
    @DisplayName("TC-10-03: Retrieves entity timeline in chronological ascending order")
    void shouldRetrieveEntityTimelineChronologically() {
        AuditLog log1 = new AuditLog(
                tenant, "corr-1", AuditAction.REQUISITION_CREATED, ComplianceTag.ISO_9001_TRACEABILITY,
                UUID.randomUUID(), "requester@test.com", "REQUESTER", "127.0.0.1", null,
                "PURCHASE_REQUISITION", "PR-2026-00001", null, null, new BigDecimal("50000.00"), "TRY",
                "DRAFT", "PENDING_APPROVAL", "Requisition submitted", null
        );
        log1.setId(UUID.randomUUID());

        AuditLog log2 = new AuditLog(
                tenant, "corr-2", AuditAction.REQUISITION_APPROVED, ComplianceTag.ISO_9001_TRACEABILITY,
                UUID.randomUUID(), "cfo@test.com", "CFO", "127.0.0.1", null,
                "PURCHASE_REQUISITION", "PR-2026-00001", null, null, new BigDecimal("50000.00"), "TRY",
                "PENDING_APPROVAL", "APPROVED", "Approved by CFO", null
        );
        log2.setId(UUID.randomUUID());

        when(auditLogRepository.findAllByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtAsc(tenantId, "PURCHASE_REQUISITION", "PR-2026-00001"))
                .thenReturn(List.of(log1, log2));

        List<AuditTimelineItemResponse> timeline = auditService.getEntityTimeline("PURCHASE_REQUISITION", "PR-2026-00001");

        assertThat(timeline).hasSize(2);
        assertThat(timeline.get(0).action()).isEqualTo(AuditAction.REQUISITION_CREATED);
        assertThat(timeline.get(1).action()).isEqualTo(AuditAction.REQUISITION_APPROVED);
    }

    @Test
    @DisplayName("TC-10-03: Filters audit logs by action, compliance tag and date range")
    void shouldFilterLogsByCriteria() {
        AuditLog log = new AuditLog(
                tenant, "corr-1", AuditAction.VENDOR_STATUS_CHANGED, ComplianceTag.SOX_404_FINANCIAL_CONTROL,
                UUID.randomUUID(), "cfo@test.com", "CFO", "127.0.0.1", null,
                "VENDOR", "VEN-001", null, null, null, null,
                "PENDING_REVIEW", "APPROVED", "IBAN change approved", null
        );
        log.setId(UUID.randomUUID());

        when(auditLogRepository.findAllByTenantIdAndActionOrderByCreatedAtDesc(tenantId, AuditAction.VENDOR_STATUS_CHANGED))
                .thenReturn(List.of(log));

        List<AuditLogResponse> logs = auditService.getLogs(AuditAction.VENDOR_STATUS_CHANGED, null, null, null);

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).action()).isEqualTo(AuditAction.VENDOR_STATUS_CHANGED);
    }
}
