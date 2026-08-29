package com.enterprise.spendsync.notification.listener;

import com.enterprise.spendsync.matching.internal.domain.InvoiceMatchStatus;
import com.enterprise.spendsync.matching.internal.event.InvoiceMatchedEvent;
import com.enterprise.spendsync.notification.api.event.BudgetThresholdExceededEvent;
import com.enterprise.spendsync.notification.api.event.PrApprovalRequestedEvent;
import com.enterprise.spendsync.notification.internal.domain.NotificationEventType;
import com.enterprise.spendsync.notification.internal.domain.NotificationReferenceType;
import com.enterprise.spendsync.notification.internal.listener.NotificationEventListener;
import com.enterprise.spendsync.notification.internal.service.NotificationService;
import com.enterprise.spendsync.payment.internal.event.PaymentDispatchedEvent;
import com.enterprise.spendsync.purchasing.internal.event.PurchaseOrderCancelledEvent;
import com.enterprise.spendsync.receiving.internal.event.GoodsReceivedEvent;
import com.enterprise.spendsync.requisition.internal.event.RequisitionApprovedEvent;
import com.enterprise.spendsync.requisition.internal.event.RequisitionRejectedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationEventListener listener;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should route PrApprovalRequestedEvent to designated approver")
    void shouldRoutePrApprovalRequestedEvent() {
        UUID reqId = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();
        UUID reqerId = UUID.randomUUID();

        PrApprovalRequestedEvent event = PrApprovalRequestedEvent.of(
                tenantId, reqId, "PR-2026-0001", reqerId, "Alice", approverId, 1,
                new BigDecimal("15000.00"), "USD", "Laptop upgrade"
        );

        listener.onPrApprovalRequested(event);

        verify(notificationService).dispatchNotification(
                eq(tenantId),
                eq(approverId),
                eq(NotificationEventType.PR_APPROVAL_REQUESTED),
                eq("Approval Requested: PR-2026-0001 (15000.00 USD)"),
                any(),
                eq(NotificationReferenceType.REQUISITION),
                eq(reqId),
                eq("pr-approval-request"),
                any()
        );
    }

    @Test
    @DisplayName("Should route RequisitionApprovedEvent to requisitioner")
    void shouldRouteRequisitionApprovedEvent() {
        UUID reqId = UUID.randomUUID();
        UUID reqerId = UUID.randomUUID();

        RequisitionApprovedEvent event = RequisitionApprovedEvent.of(
                tenantId, reqId, "PR-2026-0001", reqerId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("15000.00"), "USD", "Title", List.of()
        );

        listener.onRequisitionApproved(event);

        verify(notificationService).dispatchNotification(
                eq(tenantId),
                eq(reqerId),
                eq(NotificationEventType.REQUISITION_APPROVED),
                eq("Requisition Approved: PR-2026-0001"),
                any(),
                eq(NotificationReferenceType.REQUISITION),
                eq(reqId),
                eq("pr-decision"),
                any()
        );
    }

    @Test
    @DisplayName("Should route RequisitionRejectedEvent to rejecter/requester")
    void shouldRouteRequisitionRejectedEvent() {
        UUID reqId = UUID.randomUUID();
        UUID rejecterId = UUID.randomUUID();

        RequisitionRejectedEvent event = RequisitionRejectedEvent.of(
                tenantId, reqId, "PR-2026-0001", rejecterId, "Budget exceeded", new BigDecimal("1000.00")
        );

        listener.onRequisitionRejected(event);

        verify(notificationService).dispatchNotification(
                eq(tenantId),
                eq(rejecterId),
                eq(NotificationEventType.REQUISITION_REJECTED),
                eq("Requisition Rejected: PR-2026-0001"),
                any(),
                eq(NotificationReferenceType.REQUISITION),
                eq(reqId),
                eq("pr-decision"),
                any()
        );
    }

    @Test
    @DisplayName("Should route PurchaseOrderCancelledEvent to canceller")
    void shouldRoutePurchaseOrderCancelledEvent() {
        UUID poId = UUID.randomUUID();
        UUID cancellerId = UUID.randomUUID();

        PurchaseOrderCancelledEvent event = PurchaseOrderCancelledEvent.of(
                tenantId, poId, "PO-2026-0001", cancellerId, "Supplier out of stock", new BigDecimal("5000.00")
        );

        listener.onPurchaseOrderCancelled(event);

        verify(notificationService).dispatchNotification(
                eq(tenantId),
                eq(cancellerId),
                eq(NotificationEventType.PURCHASE_ORDER_CANCELLED),
                eq("Purchase Order Cancelled: PO-2026-0001"),
                any(),
                eq(NotificationReferenceType.PURCHASE_ORDER),
                eq(poId),
                eq(null),
                eq(null)
        );
    }

    @Test
    @DisplayName("Should route GoodsReceivedEvent to receiving user")
    void shouldRouteGoodsReceivedEvent() {
        UUID grId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        GoodsReceivedEvent event = new GoodsReceivedEvent(
                tenantId, grId, "GRN-2026-0001", UUID.randomUUID(), "PO-2026-0001",
                UUID.randomUUID(), "WB-9912", receiverId, List.of(), Instant.now()
        );

        listener.onGoodsReceived(event);

        verify(notificationService).dispatchNotification(
                eq(tenantId),
                eq(receiverId),
                eq(NotificationEventType.GOODS_RECEIVED),
                eq("Goods Receipt Logged: GRN-2026-0001"),
                any(),
                eq(NotificationReferenceType.GOODS_RECEIPT),
                eq(grId),
                eq(null),
                eq(null)
        );
    }

    @Test
    @DisplayName("Should route PaymentDispatchedEvent to authorizer")
    void shouldRoutePaymentDispatchedEvent() {
        UUID batchId = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();

        PaymentDispatchedEvent event = new PaymentDispatchedEvent(
                tenantId, batchId, "BATCH-2026-0001", UUID.randomUUID(), new BigDecimal("100000.00"),
                "USD", 15, approverId, List.of(), Instant.now()
        );

        listener.onPaymentDispatched(event);

        verify(notificationService).dispatchNotification(
                eq(tenantId),
                eq(approverId),
                eq(NotificationEventType.PAYMENT_DISPATCHED),
                eq("Payment Batch Dispatched: BATCH-2026-0001"),
                any(),
                eq(NotificationReferenceType.PAYMENT_BATCH),
                eq(batchId),
                eq("payment-processed"),
                any()
        );
    }
}
