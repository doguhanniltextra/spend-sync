package com.enterprise.spendsync.vendorportal.dto;

import com.enterprise.spendsync.matching.internal.domain.InvoiceProfile;
import com.enterprise.spendsync.matching.internal.domain.InvoiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PoFlipInvoiceRequest(
        @NotBlank(message = "Invoice number is required")
        @Size(max = 50)
        String invoiceNumber,

        @NotBlank(message = "ETTN (UUID) is required")
        @Size(max = 100)
        String ettn,

        InvoiceProfile profileId,

        InvoiceType invoiceType,

        @NotNull(message = "Invoice date is required")
        LocalDate invoiceDate,

        @NotEmpty(message = "At least one line item must be invoiced")
        @Valid
        List<PoFlipLineItemDto> lineItems
) {
    public record PoFlipLineItemDto(
            @NotNull(message = "Purchase Order Line Item ID is required")
            UUID purchaseOrderLineItemId,

            @NotNull(message = "Invoiced quantity is required")
            BigDecimal invoicedQuantity,

            BigDecimal taxRate,

            String tevkifatCode,
            String tevkifatRate
    ) {}
}
