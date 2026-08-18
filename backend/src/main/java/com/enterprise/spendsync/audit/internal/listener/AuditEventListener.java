package com.enterprise.spendsync.audit.internal.listener;

import com.enterprise.spendsync.audit.internal.domain.AuditAction;
import com.enterprise.spendsync.audit.internal.domain.ComplianceTag;
import com.enterprise.spendsync.audit.internal.dto.RecordAuditRequest;
import com.enterprise.spendsync.audit.internal.service.AuditService;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.purchasing.internal.event.PurchaseOrderCancelledEvent;
import com.enterprise.spendsync.purchasing.internal.event.PurchaseOrderIssuedEvent;
import com.enterprise.spendsync.purchasing.internal.event.PurchaseOrderRevisedEvent;
import com.enterprise.spendsync.requisition.internal.event.RequisitionApprovedEvent;
import com.enterprise.spendsync.requisition.internal.event.RequisitionRejectedEvent;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Asynchronous Audit Event Listener.
 * Consumes domain events across P2P modules and persists them into the immutable audit trail.
 */
@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final AuditService auditService;
    private final TenantRepository tenantRepository;

    public AuditEventListener(AuditService auditService, TenantRepository tenantRepository) {
        this.auditService = auditService;
        this.tenantRepository = tenantRepository;
    }

    @EventListener
    public void onRequisitionApproved(RequisitionApprovedEvent event) {
        runWithTenant(event.tenantId(), () -> {
            log.info("Audit trail recording REQUISITION_APPROVED for PR: {}", event.requisitionNumber());
            auditService.recordAuditLog(new RecordAuditRequest(
                    UUID.randomUUID().toString(),
                    AuditAction.REQUISITION_APPROVED,
                    ComplianceTag.ISO_9001_TRACEABILITY,
                    event.requisitionerId(),
                    null,
                    "REQUISITIONER",
                    "127.0.0.1",
                    "SpringDomainEventBus",
                    "PURCHASE_REQUISITION",
                    event.requisitionNumber(),
                    event.legalEntityId(),
                    event.costCenterId(),
                    event.totalAmount(),
                    event.currency(),
                    "PENDING_APPROVAL",
                    "APPROVED",
                    "Requisition approval chain completed successfully.",
                    "{\"prId\":\"" + event.requisitionId() + "\",\"title\":\"" + event.title() + "\"}"
            ));
        });
    }

    @EventListener
    public void onRequisitionRejected(RequisitionRejectedEvent event) {
        runWithTenant(event.tenantId(), () -> {
            log.info("Audit trail recording REQUISITION_REJECTED for PR: {}", event.requisitionNumber());
            auditService.recordAuditLog(new RecordAuditRequest(
                    UUID.randomUUID().toString(),
                    AuditAction.REQUISITION_REJECTED,
                    ComplianceTag.ISO_37001_ANTI_BRIBERY,
                    event.rejectedByUserId(),
                    null,
                    "APPROVER",
                    "127.0.0.1",
                    "SpringDomainEventBus",
                    "PURCHASE_REQUISITION",
                    event.requisitionNumber(),
                    null,
                    null,
                    event.releasedBudgetAmount(),
                    "TRY",
                    "PENDING_APPROVAL",
                    "REJECTED",
                    "Rejection reason: " + event.rejectionReason(),
                    "{\"prId\":\"" + event.requisitionId() + "\",\"releasedAmount\":" + event.releasedBudgetAmount() + "}"
            ));
        });
    }

    @EventListener
    public void onPurchaseOrderIssued(PurchaseOrderIssuedEvent event) {
        runWithTenant(event.tenantId(), () -> {
            log.info("Audit trail recording PURCHASE_ORDER_ISSUED for PO: {}", event.poNumber());
            auditService.recordAuditLog(new RecordAuditRequest(
                    UUID.randomUUID().toString(),
                    AuditAction.PURCHASE_ORDER_ISSUED,
                    ComplianceTag.ISO_9001_TRACEABILITY,
                    null,
                    event.vendorOrderEmail(),
                    "PROCUREMENT",
                    "127.0.0.1",
                    "SpringDomainEventBus",
                    "PURCHASE_ORDER",
                    event.poNumber(),
                    event.legalEntityId(),
                    event.costCenterId(),
                    event.totalAmount(),
                    event.currency(),
                    "DRAFT",
                    "ISSUED",
                    "PO issued to vendor " + event.vendorName() + " with Incoterms " + event.incoterms(),
                    "{\"poId\":\"" + event.poId() + "\",\"vendorId\":\"" + event.vendorId() + "\",\"incoterms\":\"" + event.incoterms() + "\"}"
            ));
        });
    }

    @EventListener
    public void onPurchaseOrderRevised(PurchaseOrderRevisedEvent event) {
        runWithTenant(event.tenantId(), () -> {
            log.info("Audit trail recording PURCHASE_ORDER_REVISED for PO: {} (Rev: {})", event.poNumber(), event.revisionNumber());
            auditService.recordAuditLog(new RecordAuditRequest(
                    UUID.randomUUID().toString(),
                    AuditAction.PURCHASE_ORDER_REVISED,
                    ComplianceTag.SOX_404_FINANCIAL_CONTROL,
                    null,
                    null,
                    "PROCUREMENT",
                    "127.0.0.1",
                    "SpringDomainEventBus",
                    "PURCHASE_ORDER",
                    event.poNumber(),
                    null,
                    null,
                    event.differentialAmount(),
                    "TRY",
                    "ISSUED",
                    "REVISED",
                    "Revision reason: " + event.reason() + " (Diff: " + event.differentialAmount() + ")",
                    "{\"poId\":\"" + event.poId() + "\",\"previousTotal\":" + event.previousTotalAmount() + ",\"newTotal\":" + event.newTotalAmount() + "}"
            ));
        });
    }

    @EventListener
    public void onPurchaseOrderCancelled(PurchaseOrderCancelledEvent event) {
        runWithTenant(event.tenantId(), () -> {
            log.info("Audit trail recording PURCHASE_ORDER_CANCELLED for PO: {}", event.poNumber());
            auditService.recordAuditLog(new RecordAuditRequest(
                    UUID.randomUUID().toString(),
                    AuditAction.PURCHASE_ORDER_CANCELLED,
                    ComplianceTag.SOX_404_FINANCIAL_CONTROL,
                    event.cancelledByUserId(),
                    null,
                    "PROCUREMENT",
                    "127.0.0.1",
                    "SpringDomainEventBus",
                    "PURCHASE_ORDER",
                    event.poNumber(),
                    null,
                    null,
                    event.releasedBudgetAmount(),
                    "TRY",
                    "ISSUED",
                    "CANCELLED",
                    "Cancellation reason: " + event.cancellationReason(),
                    "{\"poId\":\"" + event.poId() + "\",\"releasedBudget\":" + event.releasedBudgetAmount() + "}"
            ));
        });
    }

    @EventListener
    public void onGoodsReceived(com.enterprise.spendsync.receiving.internal.event.GoodsReceivedEvent event) {
        runWithTenant(event.tenantId(), () -> {
            log.info("Audit trail recording GOODS_RECEIPT_CREATED for GR: {} (PO: {})", event.receiptNumber(), event.poNumber());
            auditService.recordAuditLog(new RecordAuditRequest(
                    UUID.randomUUID().toString(),
                    AuditAction.GOODS_RECEIPT_CREATED,
                    ComplianceTag.ISO_9001_TRACEABILITY,
                    event.receivedByUserId(),
                    null,
                    "FACILITY_USER",
                    "127.0.0.1",
                    "SpringDomainEventBus",
                    "GOODS_RECEIPT",
                    event.receiptNumber(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    "COMPLETED",
                    "Goods received against PO " + event.poNumber() + " with Waybill " + event.waybillNumber(),
                    "{\"grId\":\"" + event.goodsReceiptId() + "\",\"poId\":\"" + event.purchaseOrderId() + "\",\"waybill\":\"" + event.waybillNumber() + "\"}"
            ));
        });
    }

    @EventListener
    public void onInvoiceMatched(com.enterprise.spendsync.matching.internal.event.InvoiceMatchedEvent event) {
        runWithTenant(event.tenantId(), () -> {
            log.info("Audit trail recording INVOICE_MATCH for Invoice: {} (Status: {})", event.invoiceNumber(), event.matchStatus());
            AuditAction action = event.matchStatus() == com.enterprise.spendsync.matching.internal.domain.InvoiceMatchStatus.AUTO_MATCHED
                    || event.matchStatus() == com.enterprise.spendsync.matching.internal.domain.InvoiceMatchStatus.MANUALLY_MATCHED
                    ? AuditAction.INVOICE_MATCH_SUCCESS
                    : AuditAction.INVOICE_MATCH_FAILED;

            auditService.recordAuditLog(new RecordAuditRequest(
                    UUID.randomUUID().toString(),
                    action,
                    ComplianceTag.SOX_404_FINANCIAL_CONTROL,
                    null,
                    null,
                    "AP_SPECIALIST",
                    "127.0.0.1",
                    "SpringDomainEventBus",
                    "SUPPLIER_INVOICE",
                    event.invoiceNumber(),
                    null,
                    null,
                    event.totalAmount(),
                    event.currency(),
                    "EVALUATING",
                    event.matchStatus().name(),
                    event.discrepancyReason() != null ? event.discrepancyReason() : "3-Way Match evaluation completed.",
                    "{\"invoiceId\":\"" + event.invoiceId() + "\",\"ettn\":\"" + event.ettn() + "\",\"poId\":\"" + event.purchaseOrderId() + "\"}"
            ));
        });
    }

    private void runWithTenant(UUID tenantId, Runnable action) {
        TenantContext.setTenantId(tenantId);
        try {
            action.run();
        } finally {
            TenantContext.clear();
        }
    }
}
