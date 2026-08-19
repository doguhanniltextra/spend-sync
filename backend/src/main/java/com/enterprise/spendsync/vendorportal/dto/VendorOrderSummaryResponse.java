package com.enterprise.spendsync.vendorportal.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record VendorOrderSummaryResponse(
        UUID id,
        String poNumber,
        BigDecimal totalAmount,
        String currency,
        String status,
        String acknowledgmentStatus,
        LocalDate promisedDeliveryDate,
        String deliveryFacilityName,
        int totalLineItems,
        Instant issuedAt,
        Instant createdAt
) {}
