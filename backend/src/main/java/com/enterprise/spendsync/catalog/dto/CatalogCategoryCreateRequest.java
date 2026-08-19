package com.enterprise.spendsync.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CatalogCategoryCreateRequest(
        UUID parentId,

        @Size(max = 100, message = "Code must be at most 100 characters")
        String code,

        @NotBlank(message = "Category name is required")
        @Size(max = 150, message = "Category name must be at most 150 characters")
        String name,

        String iconCode,

        String description
) {
}
