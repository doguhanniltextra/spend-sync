package com.enterprise.spendsync.catalog.internal.service;

import com.enterprise.spendsync.catalog.dto.CatalogAutofillResponse;
import com.enterprise.spendsync.catalog.dto.CatalogCategoryDto;
import com.enterprise.spendsync.catalog.dto.CatalogHealthMetricsDto;
import com.enterprise.spendsync.catalog.dto.CatalogItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CatalogService {

    Page<CatalogItemResponse> searchCatalogItems(UUID tenantId, String query, UUID categoryId, Boolean activeOnly, Pageable pageable);

    CatalogItemResponse getCatalogItemById(UUID tenantId, UUID id);

    CatalogAutofillResponse getCatalogItemAutofill(UUID tenantId, UUID itemId);

    List<CatalogCategoryDto> getCategoryTree(UUID tenantId);

    CatalogHealthMetricsDto getCatalogHealthMetrics(UUID tenantId);
}
