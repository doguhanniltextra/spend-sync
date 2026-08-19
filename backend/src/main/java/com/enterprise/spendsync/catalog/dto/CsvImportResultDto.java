package com.enterprise.spendsync.catalog.dto;

import java.util.List;

public record CsvImportResultDto(
        int totalRows,
        int successCount,
        int failureCount,
        List<CsvRowErrorDto> errors
) {
}
