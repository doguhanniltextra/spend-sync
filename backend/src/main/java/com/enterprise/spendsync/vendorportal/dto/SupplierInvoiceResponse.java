package com.enterprise.spendsync.vendorportal.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SupplierInvoiceResponse(
        UUID id,
        UUID purchaseOrderId,
        String poNumber,
        String invoiceNumber,
        String ettn,
        String profileId,
        String invoiceType,
        LocalDate invoiceDate,
        LocalDate dueDate,
        String currency,
        BigDecimal subtotalAmount,
        BigDecimal taxAmount,
        BigDecimal withholdingTaxAmount,
        BigDecimal totalAmount,
        BigDecimal payableAmount,
        String matchType,
        String matchStatus,
        String status,
        String rejectionReason,
        Instant createdAt
) {}
