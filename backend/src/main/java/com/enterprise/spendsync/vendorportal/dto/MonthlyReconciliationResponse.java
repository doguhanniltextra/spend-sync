package com.enterprise.spendsync.vendorportal.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MonthlyReconciliationResponse(
        UUID id,
        UUID vendorId,
        String vendorName,
        int year,
        int month,
        int invoiceCount,
        BigDecimal totalAmount,
        String currency,
        String status,
        String vendorNotes,
        Instant vendorApprovedAt,
        String signedChecksum
) {}
