package com.enterprise.spendsync.catalog.dto;

import java.util.List;
import java.util.UUID;

public record CatalogCategoryDto(
        UUID id,
        String code,
        String name,
        String fullPath,
        String iconCode,
        String description,
        UUID parentId,
        long itemCount,
        List<CatalogCategoryDto> children
) {
}
