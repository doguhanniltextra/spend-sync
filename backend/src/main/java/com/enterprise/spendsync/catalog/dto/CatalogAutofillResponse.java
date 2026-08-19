package com.enterprise.spendsync.catalog.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogAutofillResponse(
        UUID itemId,
        String itemCode,
        String itemName,
        LineItemSuggestion lineItemSuggestion,
        SuggestedVendor suggestedVendor,
        BudgetHint budgetHint,
        String contractAlert
) {
    public record LineItemSuggestion(
            String description,
            String categoryCode,
            String categoryFullPath,
            BigDecimal quantity,
            String unitOfMeasure,
            BigDecimal unitPrice,
            BigDecimal vatRate,
            BigDecimal lineTotal
    ) {}

    public record SuggestedVendor(
            UUID vendorId,
            String vendorName,
            String taxNumber,
            String orderEmail,
            String paymentTerms
    ) {}

    public record BudgetHint(
            String glAccountCode,
            UUID suggestedCostCenterId
    ) {}
}
