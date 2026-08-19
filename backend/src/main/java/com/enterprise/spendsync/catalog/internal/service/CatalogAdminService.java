package com.enterprise.spendsync.catalog.internal.service;

import com.enterprise.spendsync.catalog.dto.CatalogCategoryCreateRequest;
import com.enterprise.spendsync.catalog.dto.CatalogCategoryDto;
import com.enterprise.spendsync.catalog.dto.CatalogItemCreateRequest;
import com.enterprise.spendsync.catalog.dto.CatalogItemResponse;
import com.enterprise.spendsync.catalog.dto.CatalogItemUpdateRequest;
import com.enterprise.spendsync.catalog.dto.CsvImportResultDto;
import com.enterprise.spendsync.core.internal.domain.User;

import java.io.InputStream;
import java.util.UUID;

public interface CatalogAdminService {

    CatalogItemResponse createCatalogItem(UUID tenantId, CatalogItemCreateRequest request, User currentUser);

    CatalogItemResponse updateCatalogItem(UUID tenantId, UUID itemId, CatalogItemUpdateRequest request);

    void deleteCatalogItem(UUID tenantId, UUID itemId);

    CatalogCategoryDto createCategory(UUID tenantId, CatalogCategoryCreateRequest request);

    CsvImportResultDto importCatalogFromCsv(UUID tenantId, InputStream csvInputStream, User currentUser);

    byte[] exportCatalogToCsv(UUID tenantId);
}
