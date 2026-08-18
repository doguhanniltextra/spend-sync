package com.enterprise.spendsync.requisition.internal.dto;

import com.enterprise.spendsync.requisition.internal.domain.PurchaseRequisition;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RequisitionSummaryResponse(
        UUID id,
        String requisitionNumber,
        String title,
        UUID requisitionerId,
        String requisitionerName,
        String costCenterName,
        String legalEntityName,
        RequisitionStatus status,
        BigDecimal totalAmount,
        String currency,
        int totalLineItems,
        boolean isCrossEntity,
        Instant createdAt
) {
    public static RequisitionSummaryResponse from(PurchaseRequisition pr) {
        String reqName = pr.getRequisitioner().getFirstName() + " " + pr.getRequisitioner().getLastName();
        boolean isCross = pr.getDeliveryFacility() != null &&
                pr.getDeliveryFacility().getLegalEntity() != null &&
                !pr.getLegalEntity().getId().equals(pr.getDeliveryFacility().getLegalEntity().getId());

        return new RequisitionSummaryResponse(
                pr.getId(),
                pr.getRequisitionNumber(),
                pr.getTitle(),
                pr.getRequisitioner().getId(),
                reqName,
                pr.getCostCenter().getName(),
                pr.getLegalEntity().getName(),
                pr.getStatus(),
                pr.getTotalAmount(),
                pr.getCurrency(),
                pr.getLineItems().size(),
                isCross,
                pr.getCreatedAt()
        );
    }
}
