package com.enterprise.spendsync.purchasing.internal.dto;

import com.enterprise.spendsync.purchasing.internal.domain.Incoterms;
import com.enterprise.spendsync.purchasing.internal.domain.PaymentTerms;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrder;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderStatus;
import com.enterprise.spendsync.shared.domain.CrossAssignmentWarning;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderDetailResponse(
        UUID id,
        String poNumber,
        int revisionNumber,
        UUID requisitionId,
        String requisitionNumber,
        UUID legalEntityId,
        String legalEntityName,
        UUID costCenterId,
        String costCenterName,
        UUID deliveryFacilityId,
        String deliveryFacilityName,
        UUID vendorId,
        String vendorName,
        String vendorTaxNumber,
        String vendorOrderEmail,
        PurchaseOrderStatus status,
        Incoterms incoterms,
        String currency,
        BigDecimal totalAmount,
        PaymentTerms paymentTerms,
        String notes,
        Instant issuedAt,
        UUID createdByUserId,
        String createdByUserName,
        List<POLineItemResponse> lineItems,
        List<PORevisionResponse> revisions,
        CrossAssignmentWarning crossAssignmentWarning,
        Instant createdAt,
        Instant updatedAt
) {
    public static PurchaseOrderDetailResponse from(PurchaseOrder po,
                                                   CrossAssignmentWarning warning,
                                                   List<PORevisionResponse> revisions) {
        String createdByName = po.getCreatedByUser() != null
                ? po.getCreatedByUser().getFirstName() + " " + po.getCreatedByUser().getLastName()
                : "System";

        List<POLineItemResponse> items = po.getLineItems().stream()
                .map(POLineItemResponse::from)
                .toList();

        return new PurchaseOrderDetailResponse(
                po.getId(),
                po.getPoNumber(),
                po.getRevisionNumber(),
                po.getRequisition() != null ? po.getRequisition().getId() : null,
                po.getRequisition() != null ? po.getRequisition().getRequisitionNumber() : null,
                po.getLegalEntity().getId(),
                po.getLegalEntity().getName(),
                po.getCostCenter().getId(),
                po.getCostCenter().getName(),
                po.getDeliveryFacility().getId(),
                po.getDeliveryFacility().getName(),
                po.getVendor().getId(),
                po.getVendor().getName(),
                po.getVendor().getTaxNumber(),
                po.getVendor().getOrderEmail(),
                po.getStatus(),
                po.getIncoterms(),
                po.getCurrency(),
                po.getTotalAmount(),
                po.getPaymentTerms(),
                po.getNotes(),
                po.getIssuedAt(),
                po.getCreatedByUser().getId(),
                createdByName,
                items,
                revisions != null ? revisions : List.of(),
                warning,
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }
}
