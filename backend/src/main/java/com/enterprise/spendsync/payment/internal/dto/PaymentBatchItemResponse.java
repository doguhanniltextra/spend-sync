package com.enterprise.spendsync.payment.internal.dto;

import com.enterprise.spendsync.payment.internal.domain.PaymentBatchItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentBatchItemResponse(
        UUID id,
        UUID supplierInvoiceId,
        String invoiceNumber,
        UUID vendorId,
        String vendorName,
        String vendorIban,
        BigDecimal amount,
        BigDecimal discountAmount,
        BigDecimal netPayableAmount,
        String status,
        Instant createdAt
) {
    public static PaymentBatchItemResponse from(PaymentBatchItem item) {
        return new PaymentBatchItemResponse(
                item.getId(),
                item.getSupplierInvoice().getId(),
                item.getSupplierInvoice().getInvoiceNumber(),
                item.getVendor().getId(),
                item.getVendorName(),
                item.getVendorIban(),
                item.getAmount(),
                item.getDiscountAmount(),
                item.getNetPayableAmount(),
                item.getStatus(),
                item.getCreatedAt()
        );
    }
}
