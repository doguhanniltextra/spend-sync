package com.enterprise.spendsync.requisition.internal.dto;

import com.enterprise.spendsync.requisition.internal.domain.ApprovalStepStatus;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionApprovalStep;

import java.time.Instant;
import java.util.UUID;

public record ApprovalStepResponse(
        UUID id,
        int stepOrder,
        UUID approverId,
        String approverName,
        String approverEmail,
        int approvalLevel,
        ApprovalStepStatus status,
        String decisionNote,
        Instant decidedAt
) {
    public static ApprovalStepResponse from(RequisitionApprovalStep step) {
        String approverName = step.getApprover().getFirstName() + " " + step.getApprover().getLastName();
        return new ApprovalStepResponse(
                step.getId(),
                step.getStepOrder(),
                step.getApprover().getId(),
                approverName,
                step.getApprover().getEmail(),
                step.getApprovalLevel(),
                step.getStatus(),
                step.getDecisionNote(),
                step.getDecidedAt()
        );
    }
}
