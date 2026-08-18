package com.enterprise.spendsync.matching.internal.dto;

import com.enterprise.spendsync.matching.internal.domain.InvoiceMatchStatus;
import com.enterprise.spendsync.matching.internal.domain.InvoiceProfile;
import com.enterprise.spendsync.matching.internal.domain.InvoiceStatus;
import com.enterprise.spendsync.matching.internal.domain.InvoiceType;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SupplierInvoiceResponse(
        UUID id,
        String invoiceNumber,
        String ettn,
        LocalDate invoiceDate,
        InvoiceType invoiceType,
        InvoiceProfile invoiceProfile,
        UUID purchaseOrderId,
        String poNumber,
        UUID vendorId,
        String vendorName,
        String vendorTaxNumber,
        UUID legalEntityId,
        String legalEntityName,
        UUID costCenterId,
        String costCenterName,
        String currency,
        BigDecimal subtotalAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        InvoiceMatchStatus matchStatus,
        InvoiceStatus status,
        String discrepancyReason,
        String managerOverrideNote,
        UUID managerOverrideByUserId,
        List<InvoiceLineItemResponse> lineItems,
        Instant createdAt
) {
    public static SupplierInvoiceResponse from(SupplierInvoice inv) {
        return new SupplierInvoiceResponse(
                inv.getId(),
                inv.getInvoiceNumber(),
                inv.getEttn(),
                inv.getInvoiceDate(),
                inv.getInvoiceType(),
                inv.getInvoiceProfile(),
                inv.getPurchaseOrder().getId(),
                inv.getPurchaseOrder().getPoNumber(),
                inv.getVendor().getId(),
                inv.getVendor().getName(),
                inv.getVendor().getTaxNumber(),
                inv.getLegalEntity().getId(),
                inv.getLegalEntity().getName(),
                inv.getCostCenter().getId(),
                inv.getCostCenter().getName(),
                inv.getCurrency(),
                inv.getSubtotalAmount(),
                inv.getTaxAmount(),
                inv.getTotalAmount(),
                inv.getMatchStatus(),
                inv.getStatus(),
                inv.getDiscrepancyReason(),
                inv.getManagerOverrideNote(),
                inv.getManagerOverrideByUser() != null ? inv.getManagerOverrideByUser().getId() : null,
                inv.getLineItems().stream().map(InvoiceLineItemResponse::from).toList(),
                inv.getCreatedAt()
        );
    }
}
