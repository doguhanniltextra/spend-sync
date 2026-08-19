package com.enterprise.spendsync.vendorportal.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SupplierInvoiceDetailResponse(
        UUID id,
        UUID purchaseOrderId,
        String poNumber,
        UUID vendorId,
        String vendorName,
        String invoiceNumber,
        String ettn,
        String profileId,
        String invoiceType,
        LocalDate invoiceDate,
        LocalDate dueDate,
        String currency,
        BigDecimal exchangeRate,
        BigDecimal subtotalAmount,
        BigDecimal taxAmount,
        BigDecimal withholdingTaxAmount,
        BigDecimal totalAmount,
        BigDecimal payableAmount,
        String matchType,
        String matchStatus,
        String status,
        String discrepancyReason,
        String rejectionReason,
        List<InvoiceLineItemDto> lineItems,
        List<InvoiceDiscrepancyDto> discrepancies,
        Instant createdAt
) {
    public record InvoiceLineItemDto(
            UUID id,
            UUID purchaseOrderLineItemId,
            Integer lineNumber,
            String itemDescription,
            BigDecimal quantity,
            String unitOfMeasure,
            BigDecimal unitPrice,
            BigDecimal taxRate,
            BigDecimal taxAmount,
            String tevkifatCode,
            String tevkifatRate,
            BigDecimal tevkifatAmount,
            BigDecimal lineTotalAmount
    ) {}

    public record InvoiceDiscrepancyDto(
            UUID id,
            String discrepancyType,
            String expectedValue,
            String actualValue,
            BigDecimal varianceAmount,
            BigDecimal variancePercentage,
            boolean resolved,
            String resolutionNotes,
            Instant createdAt
    ) {}
}
