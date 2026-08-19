package com.enterprise.spendsync.vendorportal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StatementOfAccountsResponse(
        UUID vendorId,
        String vendorName,
        BigDecimal totalInvoiced,
        BigDecimal totalPaid,
        BigDecimal openBalance,
        String currency,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        List<SoaEntryDto> entries
) {
    public record SoaEntryDto(
            LocalDate date,
            String documentType,
            String documentNumber,
            String referenceNumber,
            BigDecimal debitAmount,
            BigDecimal creditAmount,
            BigDecimal runningBalance,
            String status
    ) {}
}
