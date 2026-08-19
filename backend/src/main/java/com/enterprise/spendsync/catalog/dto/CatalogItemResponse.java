package com.enterprise.spendsync.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CatalogItemResponse(
        UUID id,
        String itemCode,
        String name,
        String description,
        UUID categoryId,
        String categoryName,
        String categoryFullPath,
        UUID preferredVendorId,
        String preferredVendorName,
        String preferredVendorTaxNumber,
        String preferredVendorTier,
        BigDecimal unitPrice,
        String currency,
        BigDecimal vatRate,
        String unitOfMeasure,
        String contractReference,
        LocalDate validFrom,
        LocalDate validUntil,
        boolean isActive,
        boolean isPreferred,
        String glAccountCode,
        String contractAlert,
        Instant createdAt,
        Instant updatedAt
) {
}
