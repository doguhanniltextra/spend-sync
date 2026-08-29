package com.enterprise.spendsync.notification.internal.scheduler;

import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.notification.internal.domain.NotificationEventType;
import com.enterprise.spendsync.notification.internal.domain.NotificationReferenceType;
import com.enterprise.spendsync.notification.internal.service.NotificationService;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionApprovalStep;
import com.enterprise.spendsync.requisition.internal.repository.RequisitionApprovalStepRepository;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enterprise Escalation Scheduler for pending procurement approvals.
 * Runs on business days at 09:00 AM to detect approval steps pending for over 48 hours
 * and escalate them to direct managers or administrative supervisors.
 */
@Component
public class StaleApprovalEscalationScheduler {

    private static final Logger log = LoggerFactory.getLogger(StaleApprovalEscalationScheduler.class);

    private final RequisitionApprovalStepRepository approvalStepRepository;
    private final NotificationService notificationService;

    public StaleApprovalEscalationScheduler(RequisitionApprovalStepRepository approvalStepRepository,
                                            NotificationService notificationService) {
        this.approvalStepRepository = approvalStepRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "${spendsync.scheduler.escalation.cron:0 0 9 * * MON-FRI}")
    @Transactional
    public void escalateStaleApprovals() {
        Instant cutoff = Instant.now().minus(48, ChronoUnit.HOURS);
        log.info("Running stale approval escalation check with cutoff: {}", cutoff);

        List<RequisitionApprovalStep> staleSteps = approvalStepRepository.findStalePendingSteps(cutoff);
        log.info("Found {} stale approval step(s) requiring escalation.", staleSteps.size());

        for (RequisitionApprovalStep step : staleSteps) {
            try {
                processEscalationForStep(step);
            } catch (Exception ex) {
                log.error("Failed to escalate approval step {} for requisition {}: {}",
                        step.getId(), step.getRequisition().getRequisitionNumber(), ex.getMessage(), ex);
            }
        }
    }

    private void processEscalationForStep(RequisitionApprovalStep step) {
        UUID tenantId = step.getTenant().getId();
        User approver = step.getApprover();
        User escalationTarget = approver.getManagerUser() != null ? approver.getManagerUser() : approver;

        String prNumber = step.getRequisition().getRequisitionNumber();
        String approverName = approver.getFirstName() + " " + approver.getLastName();

        String title = String.format("⚠️ Escalation: PR %s has been pending approval for over 48 hours", prNumber);
        String body = String.format("Purchase Requisition %s assigned to %s has been pending approval for more than 48 hours. Please review or reassign.",
                prNumber, approverName);

        Map<String, Object> model = Map.of(
                "prNumber", prNumber,
                "approverName", approverName,
                "totalAmount", step.getRequisition().getTotalAmount() + " " + step.getRequisition().getCurrency(),
                "actionUrl", "/procurement/requisitions/" + step.getRequisition().getId()
        );

        TenantContext.setTenantId(tenantId);
        try {
            notificationService.dispatchNotification(
                    tenantId,
                    escalationTarget.getId(),
                    NotificationEventType.STALE_APPROVAL_ESCALATION,
                    title,
                    body,
                    NotificationReferenceType.REQUISITION,
                    step.getRequisition().getId(),
                    "stale-approval-escalation",
                    model
            );
            log.info("Escalation notification dispatched to user {} for PR {}", escalationTarget.getId(), prNumber);
        } finally {
            TenantContext.clear();
        }
    }
}
