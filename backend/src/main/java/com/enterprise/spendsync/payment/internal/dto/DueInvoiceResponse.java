package com.enterprise.spendsync.payment.internal.dto;

import com.enterprise.spendsync.matching.internal.domain.SupplierInvoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DueInvoiceResponse(
        UUID id,
        String invoiceNumber,
        String ettn,
        LocalDate invoiceDate,
        UUID vendorId,
        String vendorName,
        String vendorIban,
        UUID legalEntityId,
        String legalEntityName,
        String currency,
        BigDecimal totalAmount,
        String matchStatus,
        String status
) {
    public static DueInvoiceResponse from(SupplierInvoice inv) {
        return new DueInvoiceResponse(
                inv.getId(),
                inv.getInvoiceNumber(),
                inv.getEttn(),
                inv.getInvoiceDate(),
                inv.getVendor().getId(),
                inv.getVendor().getName(),
                inv.getVendor().getIban(),
                inv.getLegalEntity().getId(),
                inv.getLegalEntity().getName(),
                inv.getCurrency(),
                inv.getTotalAmount(),
                inv.getMatchStatus().name(),
                inv.getStatus().name()
        );
    }
}
