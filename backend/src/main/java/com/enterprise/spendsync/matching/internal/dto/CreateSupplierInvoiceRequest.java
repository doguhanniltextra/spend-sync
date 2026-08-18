package com.enterprise.spendsync.matching.internal.dto;

import com.enterprise.spendsync.matching.internal.domain.InvoiceProfile;
import com.enterprise.spendsync.matching.internal.domain.InvoiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateSupplierInvoiceRequest(
        @NotNull(message = "Purchase order ID is mandatory")
        UUID purchaseOrderId,

        @NotBlank(message = "Invoice number is mandatory")
        String invoiceNumber,

        @NotBlank(message = "ETTN is mandatory")
        String ettn,

        @NotNull(message = "Invoice date is mandatory")
        LocalDate invoiceDate,

        InvoiceType invoiceType,

        InvoiceProfile invoiceProfile,

        @NotEmpty(message = "At least one line item is required")
        @Valid
        List<CreateInvoiceLineItemRequest> lineItems
) {}
