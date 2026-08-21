package com.enterprise.spendsync.audit.listener;

import com.enterprise.spendsync.audit.internal.domain.AuditAction;
import com.enterprise.spendsync.audit.internal.domain.ComplianceTag;
import com.enterprise.spendsync.audit.internal.dto.RecordAuditRequest;
import com.enterprise.spendsync.audit.internal.listener.AuditEventListener;
import com.enterprise.spendsync.audit.internal.service.AuditService;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.matching.internal.domain.InvoiceMatchStatus;
import com.enterprise.spendsync.matching.internal.event.InvoiceMatchedEvent;
import com.enterprise.spendsync.payment.internal.event.PaymentDispatchedEvent;
import com.enterprise.spendsync.purchasing.internal.domain.Incoterms;
import com.enterprise.spendsync.purchasing.internal.event.PurchaseOrderCancelledEvent;
import com.enterprise.spendsync.purchasing.internal.event.PurchaseOrderIssuedEvent;
import com.enterprise.spendsync.purchasing.internal.event.PurchaseOrderRevisedEvent;
import com.enterprise.spendsync.receiving.internal.event.GoodsReceivedEvent;
import com.enterprise.spendsync.requisition.internal.event.RequisitionApprovedEvent;
import com.enterprise.spendsync.requisition.internal.event.RequisitionRejectedEvent;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditEventListener Asynchronous Domain Event Integration Tests (ISO 27001 / SOX 404)")
class AuditEventListenerTest {

    @Mock
    private AuditService auditService;
    @Mock
    private TenantRepository tenantRepository;

    @Captor
    private ArgumentCaptor<RecordAuditRequest> auditRequestCaptor;

    private AuditEventListener eventListener;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        eventListener = new AuditEventListener(auditService, tenantRepository);
        tenantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("TC-10-01: onRequisitionApproved captures ISO 9001 audit log on approval completion")
    void shouldRecordAuditOnRequisitionApproved() {
        RequisitionApprovedEvent event = RequisitionApprovedEvent.of(
                tenantId, UUID.randomUUID(), "PR-2026-00001", UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("75000.00"), "TRY", "MacBook Pro M3", List.of()
        );

        eventListener.onRequisitionApproved(event);

        verify(auditService).recordAuditLog(auditRequestCaptor.capture());
        RecordAuditRequest req = auditRequestCaptor.getValue();

        assertThat(req.action()).isEqualTo(AuditAction.REQUISITION_APPROVED);
        assertThat(req.complianceTag()).isEqualTo(ComplianceTag.ISO_9001_TRACEABILITY);
        assertThat(req.entityType()).isEqualTo("PURCHASE_REQUISITION");
        assertThat(req.entityId()).isEqualTo("PR-2026-00001");
        assertThat(req.fromStatus()).isEqualTo("PENDING_APPROVAL");
        assertThat(req.toStatus()).isEqualTo("APPROVED");
        assertThat(req.amount()).isEqualByComparingTo("75000.00");
    }

    @Test
    @DisplayName("TC-10-01: onRequisitionRejected captures ISO 37001 anti-bribery audit log with rejection reason")
    void shouldRecordAuditOnRequisitionRejected() {
        UUID rejectorId = UUID.randomUUID();
        RequisitionRejectedEvent event = RequisitionRejectedEvent.of(
                tenantId, UUID.randomUUID(), "PR-2026-00002", rejectorId,
                "Budget overspent for current quarter", new BigDecimal("50000.00")
        );

        eventListener.onRequisitionRejected(event);

        verify(auditService).recordAuditLog(auditRequestCaptor.capture());
        RecordAuditRequest req = auditRequestCaptor.getValue();

        assertThat(req.action()).isEqualTo(AuditAction.REQUISITION_REJECTED);
        assertThat(req.complianceTag()).isEqualTo(ComplianceTag.ISO_37001_ANTI_BRIBERY);
        assertThat(req.actorId()).isEqualTo(rejectorId);
        assertThat(req.fromStatus()).isEqualTo("PENDING_APPROVAL");
        assertThat(req.toStatus()).isEqualTo("REJECTED");
        assertThat(req.decisionNote()).contains("Budget overspent for current quarter");
    }

    @Test
    @DisplayName("TC-10-01: onPurchaseOrderIssued records PO dispatch event with Incoterms and vendor details")
    void shouldRecordAuditOnPurchaseOrderIssued() {
        PurchaseOrderIssuedEvent event = PurchaseOrderIssuedEvent.of(
                tenantId, UUID.randomUUID(), "PO-2026-00001", 1, UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Global Tech A.S.", "orders@globaltech.com", Incoterms.DAP,
                new BigDecimal("120000.00"), "TRY", List.of()
        );

        eventListener.onPurchaseOrderIssued(event);

        verify(auditService).recordAuditLog(auditRequestCaptor.capture());
        RecordAuditRequest req = auditRequestCaptor.getValue();

        assertThat(req.action()).isEqualTo(AuditAction.PURCHASE_ORDER_ISSUED);
        assertThat(req.complianceTag()).isEqualTo(ComplianceTag.ISO_9001_TRACEABILITY);
        assertThat(req.entityId()).isEqualTo("PO-2026-00001");
        assertThat(req.fromStatus()).isEqualTo("DRAFT");
        assertThat(req.toStatus()).isEqualTo("ISSUED");
    }

    @Test
    @DisplayName("TC-10-01: onPurchaseOrderRevised records SOX 404 audit log with price differential")
    void shouldRecordAuditOnPurchaseOrderRevised() {
        PurchaseOrderRevisedEvent event = PurchaseOrderRevisedEvent.of(
                tenantId, UUID.randomUUID(), "PO-2026-00001", 2,
                new BigDecimal("100000.00"), new BigDecimal("115000.00"), new BigDecimal("15000.00"),
                "Scope expansion"
        );

        eventListener.onPurchaseOrderRevised(event);

        verify(auditService).recordAuditLog(auditRequestCaptor.capture());
        RecordAuditRequest req = auditRequestCaptor.getValue();

        assertThat(req.action()).isEqualTo(AuditAction.PURCHASE_ORDER_REVISED);
        assertThat(req.complianceTag()).isEqualTo(ComplianceTag.SOX_404_FINANCIAL_CONTROL);
        assertThat(req.amount()).isEqualByComparingTo("15000.00");
    }

    @Test
    @DisplayName("TC-10-01: onPurchaseOrderCancelled records budget release in audit log")
    void shouldRecordAuditOnPurchaseOrderCancelled() {
        UUID cancellerId = UUID.randomUUID();
        PurchaseOrderCancelledEvent event = PurchaseOrderCancelledEvent.of(
                tenantId, UUID.randomUUID(), "PO-2026-00001", cancellerId,
                "Vendor unable to deliver", new BigDecimal("120000.00")
        );

        eventListener.onPurchaseOrderCancelled(event);

        verify(auditService).recordAuditLog(auditRequestCaptor.capture());
        RecordAuditRequest req = auditRequestCaptor.getValue();

        assertThat(req.action()).isEqualTo(AuditAction.PURCHASE_ORDER_CANCELLED);
        assertThat(req.fromStatus()).isEqualTo("ISSUED");
        assertThat(req.toStatus()).isEqualTo("CANCELLED");
        assertThat(req.decisionNote()).contains("Vendor unable to deliver");
    }

    @Test
    @DisplayName("TC-10-01: onGoodsReceived records warehouse receiving audit log")
    void shouldRecordAuditOnGoodsReceived() {
        GoodsReceivedEvent event = new GoodsReceivedEvent(
                tenantId, UUID.randomUUID(), "GR-2026-00001", UUID.randomUUID(),
                "PO-2026-00001", UUID.randomUUID(), "IRS-987654", UUID.randomUUID(),
                List.of(), Instant.now()
        );

        eventListener.onGoodsReceived(event);

        verify(auditService).recordAuditLog(auditRequestCaptor.capture());
        RecordAuditRequest req = auditRequestCaptor.getValue();

        assertThat(req.action()).isEqualTo(AuditAction.GOODS_RECEIPT_CREATED);
        assertThat(req.entityId()).isEqualTo("GR-2026-00001");
        assertThat(req.toStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("TC-10-01: onInvoiceMatched records 3-Way Match result audit log")
    void shouldRecordAuditOnInvoiceMatched() {
        InvoiceMatchedEvent event = new InvoiceMatchedEvent(
                tenantId, UUID.randomUUID(), "GIB2026000000001", "ettn-123",
                UUID.randomUUID(), "PO-2026-00001", UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("120000.00"), "TRY", InvoiceMatchStatus.AUTO_MATCHED, null, Instant.now()
        );

        eventListener.onInvoiceMatched(event);

        verify(auditService).recordAuditLog(auditRequestCaptor.capture());
        RecordAuditRequest req = auditRequestCaptor.getValue();

        assertThat(req.action()).isEqualTo(AuditAction.INVOICE_MATCH_SUCCESS);
        assertThat(req.complianceTag()).isEqualTo(ComplianceTag.SOX_404_FINANCIAL_CONTROL);
        assertThat(req.entityId()).isEqualTo("GIB2026000000001");
    }

    @Test
    @DisplayName("TC-10-01: onPaymentDispatched records payment batch disbursement in audit log")
    void shouldRecordAuditOnPaymentDispatched() {
        UUID approverId = UUID.randomUUID();
        PaymentDispatchedEvent event = new PaymentDispatchedEvent(
                tenantId, UUID.randomUUID(), "PAY-2026-00001", UUID.randomUUID(),
                new BigDecimal("450000.00"), "TRY", 5, approverId, List.of(), Instant.now()
        );

        eventListener.onPaymentDispatched(event);

        verify(auditService).recordAuditLog(auditRequestCaptor.capture());
        RecordAuditRequest req = auditRequestCaptor.getValue();

        assertThat(req.action()).isEqualTo(AuditAction.BUDGET_COMMITTED);
        assertThat(req.complianceTag()).isEqualTo(ComplianceTag.SOX_404_FINANCIAL_CONTROL);
        assertThat(req.entityId()).isEqualTo("PAY-2026-00001");
        assertThat(req.amount()).isEqualByComparingTo("450000.00");
    }
}
