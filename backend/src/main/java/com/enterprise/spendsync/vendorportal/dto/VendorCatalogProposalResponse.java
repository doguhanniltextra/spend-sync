package com.enterprise.spendsync.vendorportal.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VendorCatalogProposalResponse(
        UUID id,
        UUID vendorId,
        String vendorName,
        UUID itemMasterId,
        String proposedItemCode,
        String proposedName,
        String proposedCategory,
        BigDecimal proposedUnitPrice,
        String proposedCurrency,
        BigDecimal vatRate,
        Integer leadTimeDays,
        String notes,
        String status,
        String buyerDecisionNotes,
        Instant createdAt
) {}
