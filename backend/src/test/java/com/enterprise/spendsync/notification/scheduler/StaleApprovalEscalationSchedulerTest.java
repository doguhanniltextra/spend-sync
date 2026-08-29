package com.enterprise.spendsync.notification.scheduler;

import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.notification.internal.domain.NotificationEventType;
import com.enterprise.spendsync.notification.internal.domain.NotificationReferenceType;
import com.enterprise.spendsync.notification.internal.scheduler.StaleApprovalEscalationScheduler;
import com.enterprise.spendsync.notification.internal.service.NotificationService;
import com.enterprise.spendsync.requisition.internal.domain.PurchaseRequisition;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionApprovalStep;
import com.enterprise.spendsync.requisition.internal.repository.RequisitionApprovalStepRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StaleApprovalEscalationSchedulerTest {

    @Mock
    private RequisitionApprovalStepRepository approvalStepRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private StaleApprovalEscalationScheduler scheduler;

    private UUID tenantId;
    private UUID managerId;
    private UUID prId;
    private Tenant tenant;
    private User approver;
    private User manager;
    private PurchaseRequisition pr;
    private RequisitionApprovalStep step;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        managerId = UUID.randomUUID();
        prId = UUID.randomUUID();

        tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn(tenantId);

        manager = mock(User.class);
        when(manager.getId()).thenReturn(managerId);

        approver = mock(User.class);
        when(approver.getFirstName()).thenReturn("Bob");
        when(approver.getLastName()).thenReturn("Director");
        when(approver.getManagerUser()).thenReturn(manager);

        pr = mock(PurchaseRequisition.class);
        when(pr.getId()).thenReturn(prId);
        when(pr.getRequisitionNumber()).thenReturn("PR-2026-0099");
        when(pr.getTotalAmount()).thenReturn(new BigDecimal("50000.00"));
        when(pr.getCurrency()).thenReturn("USD");

        step = mock(RequisitionApprovalStep.class);
        when(step.getTenant()).thenReturn(tenant);
        when(step.getApprover()).thenReturn(approver);
        when(step.getRequisition()).thenReturn(pr);
    }

    @Test
    @DisplayName("Should detect stale steps over 48h and escalate to approver's direct manager")
    void shouldEscalateStaleStepsToDirectManager() {
        when(approvalStepRepository.findStalePendingSteps(any())).thenReturn(List.of(step));

        scheduler.escalateStaleApprovals();

        verify(notificationService, times(1)).dispatchNotification(
                eq(tenantId),
                eq(managerId),
                eq(NotificationEventType.STALE_APPROVAL_ESCALATION),
                eq("⚠️ Escalation: PR PR-2026-0099 has been pending approval for over 48 hours"),
                any(),
                eq(NotificationReferenceType.REQUISITION),
                eq(prId),
                eq("stale-approval-escalation"),
                any()
        );
    }

    @Test
    @DisplayName("Should fallback to approver if direct manager is not configured")
    void shouldFallbackToApproverWhenNoManager() {
        UUID fallbackApproverId = UUID.randomUUID();
        when(approver.getManagerUser()).thenReturn(null);
        when(approver.getId()).thenReturn(fallbackApproverId);
        when(approvalStepRepository.findStalePendingSteps(any())).thenReturn(List.of(step));

        scheduler.escalateStaleApprovals();

        verify(notificationService, times(1)).dispatchNotification(
                eq(tenantId),
                eq(fallbackApproverId),
                eq(NotificationEventType.STALE_APPROVAL_ESCALATION),
                any(),
                any(),
                eq(NotificationReferenceType.REQUISITION),
                any(),
                eq("stale-approval-escalation"),
                any()
        );
    }

    @Test
    @DisplayName("Should do nothing when no stale steps exist")
    void shouldDoNothingWhenNoStaleSteps() {
        when(approvalStepRepository.findStalePendingSteps(any())).thenReturn(List.of());

        scheduler.escalateStaleApprovals();

        verifyNoInteractions(notificationService);
    }
}
