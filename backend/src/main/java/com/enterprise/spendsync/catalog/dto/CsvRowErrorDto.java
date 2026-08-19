package com.enterprise.spendsync.catalog.dto;

public record CsvRowErrorDto(
        int rowNumber,
        String itemCodeOrName,
        String errorMessage
) {
}
