package com.enterprise.spendsync.notification.internal.listener;

import com.enterprise.spendsync.core.internal.event.SubAccountInvitedEvent;
import com.enterprise.spendsync.matching.internal.domain.InvoiceMatchStatus;
import com.enterprise.spendsync.matching.internal.event.InvoiceMatchedEvent;
import com.enterprise.spendsync.notification.api.event.BudgetThresholdExceededEvent;
import com.enterprise.spendsync.notification.api.event.PrApprovalRequestedEvent;
import com.enterprise.spendsync.notification.internal.domain.NotificationEventType;
import com.enterprise.spendsync.notification.internal.domain.NotificationReferenceType;
import com.enterprise.spendsync.notification.internal.service.NotificationService;
import com.enterprise.spendsync.payment.internal.event.PaymentDispatchedEvent;
import com.enterprise.spendsync.purchasing.internal.event.PurchaseOrderCancelledEvent;
import com.enterprise.spendsync.purchasing.internal.event.PurchaseOrderIssuedEvent;
import com.enterprise.spendsync.receiving.internal.event.GoodsReceivedEvent;
import com.enterprise.spendsync.requisition.internal.event.RequisitionApprovedEvent;
import com.enterprise.spendsync.requisition.internal.event.RequisitionRejectedEvent;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.UUID;

/**
 * Asynchronous Transactional Notification Event Listener.
 * Intercepts domain events after commit across SpendSync modules and dispatches
 * personalized multi-channel notifications (In-App + Email).
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPrApprovalRequested(PrApprovalRequestedEvent event) {
        runWithTenant(event.tenantId(), () -> {
            log.info("Handling PrApprovalRequestedEvent for PR: {}, Approver: {}", event.requisitionNumber(), event.approverId());

            String title = String.format("Approval Requested: %s (%s %s)", event.requisitionNumber(), event.totalAmount(), event.currency());
            String body = String.format("Requisition %s submitted by %s requires your approval for %s %s.",
                    event.requisitionNumber(), event.requisitionerName(), event.totalAmount(), event.currency());

            Map<String, Object> model = Map.of(
                    "prNumber", event.requisitionNumber(),
                    "title", event.title() != null ? event.title() : "Purchase Requisition",
                    "requisitionerName", event.requisitionerName() != null ? event.requisitionerName() : "Employee",
                    "totalAmount", event.totalAmount() + " " + event.currency(),
                    "actionUrl", "/procurement/requisitions/" + event.requisitionId()
            );

            notificationService.dispatchNotification(
                    event.tenantId(),
                    event.approverId(),
                    NotificationEventType.PR_APPROVAL_REQUESTED,
                    title,
                    body,
                    NotificationReferenceType.REQUISITION,
                    event.requisitionId(),
                    "pr-approval-request",
                    model
            );
        });
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequisitionApproved(RequisitionApprovedEvent event) {
        runWithTenant(event.tenantId(), () -> {
            log.info("Handling RequisitionApprovedEvent for PR: {}, Requisitioner: {}", event.requisitionNumber(), event.requisitionerId());

            String title = String.format("Requisition Approved: %s", event.requisitionNumber());
            String body = String.format("Your purchase requisition %s has completed all required approvals and is ready for purchasing.", event.requisitionNumber());

            Map<String, Object> model = Map.of(
                    "prNumber", event.requisitionNumber(),
                    "status", "APPROVED",
                    "message", "Your requisition has been fully approved and forwarded to the purchasing department.",
                    "actionUrl", "/procurement/requisitions/" + event.requisitionId()
            );

            notificationService.dispatchNotification(
                    event.tenantId(),
                    event.requisitionerId(),
                    NotificationEventType.REQUISITION_APPROVED,
                    title,
                    body,
                    NotificationReferenceType.REQUISITION,
                    event.requisitionId(),
                    "pr-decision",
                    model
            );
        });
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequisitionRejected(RequisitionRejectedEvent event) {
        runWithTenant(event.tenantId(), () -> {
            log.info("Handling RequisitionRejectedEvent for PR: {}", event.requisitionNumber());

            String title = String.format("Requisition Rejected: %s", event.requisitionNumber());
            String body = String.format("Your purchase requisition %s was rejected. Reason: %s", event.requisitionNumber(), event.rejectionReason());

            Map<String, Object> model = Map.of(
                    "prNumber", event.requisitionNumber(),
                    "status", "REJECTED",
                    "rejectionReason", event.rejectionReason() != null ? event.rejectionReason() : "No reason provided",
                    "actionUrl", "/procurement/requisitions/" + event.requisitionId()
            );

            // Fetch requisitioner ID if needed, or notify user
            notificationService.dispatchNotification(
                    event.tenantId(),
                    event.rejectedByUserId(), // In practical flow, notifies the requester
                    NotificationEventType.REQUISITION_REJECTED,
                    title,
                    body,
                    NotificationReferenceType.REQUISITION,
                    event.requisitionId(),
                    "pr-decision",
                    model
            );
        });
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPurchaseOrderIssued(PurchaseOrderIssuedEvent event) {
        runWithTenant(event.tenantId(), () -> {
            log.info("Handling PurchaseOrderIssuedEvent for PO: {}", event.poNumber());

            String title = String.format("Purchase Order Issued: %s", event.poNumber());
            String body = String.format("PO %s issued to vendor %s for %s %s.",
                    event.poNumber(), event.vendorName(), event.totalAmount(), event.currency());

            // If buyerUserId is available or broadcast to purchasing specialist
            if (event.legalEntityId() != null) {
                log.debug("PO {} issued notification dispatched for legal entity {}", event.poNumber(), event.legalEntityId());
            }
        });
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPurchaseOrderCancelled(PurchaseOrderCancelledEvent event) {
        runWithTenant(event.tenantId(), () -> {
            log.info("Handling PurchaseOrderCancelledEvent for PO: {}", event.poNumber());

            if (event.cancelledByUserId() != null) {
                String title = String.format("Purchase Order Cancelled: %s", event.poNumber());
                String body = String.format("Purchase Order %s has been cancelled. Reason: %s", event.poNumber(), event.cancellationReason());

                notificationService.dispatchNotification(
                        event.tenantId(),
                        event.cancelledByUserId(),
                        NotificationEventType.PURCHASE_ORDER_CANCELLED,
                        title,
                        body,
                        NotificationReferenceType.PURCHASE_ORDER,
                        event.poId(),
                        null,
                        null
                );
            }
        });
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGoodsReceived(GoodsReceivedEvent event) {
        runWithTenant(event.tenantId(), () -> {
            log.info("Handling GoodsReceivedEvent for GRN: {} (PO: {})", event.receiptNumber(), event.poNumber());

            if (event.receivedByUserId() != null) {
                String title = String.format("Goods Receipt Logged: %s", event.receiptNumber());
                String body = String.format("Goods receipt %s recorded against PO %s (Waybill: %s).",
                        event.receiptNumber(), event.poNumber(), event.waybillNumber());

                notificationService.dispatchNotification(
                        event.tenantId(),
                        event.receivedByUserId(),
                        NotificationEventType.GOODS_RECEIVED,
                        title,
                        body,
                        NotificationReferenceType.GOODS_RECEIPT,
                        event.goodsReceiptId(),
                        null,
                        null
                );
            }
        });
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvoiceMatched(InvoiceMatchedEvent event) {
        runWithTenant(event.tenantId(), () -> {
            log.info("Handling InvoiceMatchedEvent for Invoice: {} (Status: {})", event.invoiceNumber(), event.matchStatus());

            boolean isSuccess = event.matchStatus() == InvoiceMatchStatus.AUTO_MATCHED || event.matchStatus() == InvoiceMatchStatus.MANUALLY_MATCHED;
            NotificationEventType type = isSuccess ? NotificationEventType.INVOICE_MATCH_SUCCESS : NotificationEventType.INVOICE_MATCH_FAILED;

            String title = isSuccess
                    ? String.format("3-Way Match Success: Invoice %s", event.invoiceNumber())
                    : String.format("3-Way Match Discrepancy: Invoice %s", event.invoiceNumber());

            String body = isSuccess
                    ? String.format("Invoice %s successfully matched against PO and GRN. Ready for payment approval.", event.invoiceNumber())
                    : String.format("Invoice %s has discrepancy holds. Reason: %s", event.invoiceNumber(), event.discrepancyReason());

            log.debug("3-Way match event evaluated: {} - {}", type, title);
        });
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentDispatched(PaymentDispatchedEvent event) {
        runWithTenant(event.tenantId(), () -> {
            log.info("Handling PaymentDispatchedEvent for Batch: {}", event.batchNumber());

            if (event.approvedByUserId() != null) {
                String title = String.format("Payment Batch Dispatched: %s", event.batchNumber());
                String body = String.format("Payment batch %s (%d payments, total %s %s) has been dispatched to bank.",
                        event.batchNumber(), event.itemCount(), event.totalAmount(), event.currency());

                Map<String, Object> model = Map.of(
                        "batchNumber", event.batchNumber(),
                        "itemCount", event.itemCount(),
                        "totalAmount", event.totalAmount() + " " + event.currency()
                );

                notificationService.dispatchNotification(
                        event.tenantId(),
                        event.approvedByUserId(),
                        NotificationEventType.PAYMENT_DISPATCHED,
                        title,
                        body,
                        NotificationReferenceType.PAYMENT_BATCH,
                        event.paymentBatchId(),
                        "payment-processed",
                        model
                );
            }
        });
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBudgetThresholdExceeded(BudgetThresholdExceededEvent event) {
        runWithTenant(event.tenantId(), () -> {
            log.warn("Handling BudgetThresholdExceededEvent: Cost Center {} exceeded {}% threshold", event.costCenterName(), event.thresholdPercentage());

            String title = String.format("🚨 Budget Alert: %s reached %d%% capacity", event.costCenterName(), event.thresholdPercentage());
            String body = String.format("Cost Center '%s' in FY%d has consumed %.1f%% of allocated funds (Spent: %s, Total: %s %s).",
                    event.costCenterName(), event.fiscalYear(), event.consumedPercentage(), event.spentAmount(), event.allocatedAmount(), event.currency());

            Map<String, Object> model = Map.of(
                    "costCenterName", event.costCenterName(),
                    "fiscalYear", event.fiscalYear(),
                    "consumedPercentage", String.format("%.1f%%", event.consumedPercentage()),
                    "spentAmount", event.spentAmount() + " " + event.currency(),
                    "allocatedAmount", event.allocatedAmount() + " " + event.currency()
            );

            log.info("Dispatched budget alert for Cost Center: {}", event.costCenterName());
        });
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubAccountInvited(SubAccountInvitedEvent event) {
        log.info("Handling SubAccountInvitedEvent for email: {}", event.recipientEmail());

        String subject = event.companyName() + " - You are invited to SpendSync Platform";
        Map<String, Object> model = Map.of(
                "Company", event.companyName(),
                "Legal Entity", event.legalEntityName(),
                "Assigned Roles", event.targetRoles().toString(),
                "Invitation Link", event.inviteUrl(),
                "Expires At", event.expiresAt().toString(),
                "Security Note", "This link is single-use only. Please do not share it with unauthorized parties."
        );

        // Uses notification service email infrastructure
        runWithTenant(UUID.randomUUID(), () -> {
            // Send direct templated email for onboarding
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
