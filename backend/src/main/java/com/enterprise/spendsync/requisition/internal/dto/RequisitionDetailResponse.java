package com.enterprise.spendsync.requisition.internal.dto;

import com.enterprise.spendsync.requisition.internal.domain.PurchaseRequisition;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionStatus;
import com.enterprise.spendsync.shared.domain.CrossAssignmentWarning;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RequisitionDetailResponse(
        UUID id,
        String requisitionNumber,
        UUID requisitionerId,
        String requisitionerName,
        String requisitionerEmail,
        UUID legalEntityId,
        String legalEntityName,
        UUID costCenterId,
        String costCenterName,
        String costCenterCode,
        UUID deliveryFacilityId,
        String deliveryFacilityName,
        UUID budgetPoolId,
        RequisitionStatus status,
        BigDecimal totalAmount,
        String currency,
        String title,
        String justification,
        String rejectionReason,
        CrossAssignmentWarning crossAssignmentWarning,
        List<LineItemResponse> lineItems,
        List<ApprovalStepResponse> approvalSteps,
        Instant createdAt,
        Instant approvedAt
) {
    public static RequisitionDetailResponse from(PurchaseRequisition pr) {
        return from(pr, null);
    }

    public static RequisitionDetailResponse from(PurchaseRequisition pr, CrossAssignmentWarning warning) {
        String reqName = pr.getRequisitioner().getFirstName() + " " + pr.getRequisitioner().getLastName();
        List<LineItemResponse> items = pr.getLineItems().stream().map(LineItemResponse::from).toList();
        List<ApprovalStepResponse> steps = pr.getApprovalSteps().stream().map(ApprovalStepResponse::from).toList();

        return new RequisitionDetailResponse(
                pr.getId(),
                pr.getRequisitionNumber(),
                pr.getRequisitioner().getId(),
                reqName,
                pr.getRequisitioner().getEmail(),
                pr.getLegalEntity().getId(),
                pr.getLegalEntity().getName(),
                pr.getCostCenter().getId(),
                pr.getCostCenter().getName(),
                pr.getCostCenter().getCode(),
                pr.getDeliveryFacility().getId(),
                pr.getDeliveryFacility().getName(),
                pr.getBudgetPool() != null ? pr.getBudgetPool().getId() : null,
                pr.getStatus(),
                pr.getTotalAmount(),
                pr.getCurrency(),
                pr.getTitle(),
                pr.getJustification(),
                pr.getRejectionReason(),
                warning,
                items,
                steps,
                pr.getCreatedAt(),
                pr.getApprovedAt()
        );
    }
}
