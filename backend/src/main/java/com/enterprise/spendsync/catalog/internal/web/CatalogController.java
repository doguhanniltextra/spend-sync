package com.enterprise.spendsync.catalog.internal.web;

import com.enterprise.spendsync.catalog.dto.CatalogAutofillResponse;
import com.enterprise.spendsync.catalog.dto.CatalogCategoryDto;
import com.enterprise.spendsync.catalog.dto.CatalogHealthMetricsDto;
import com.enterprise.spendsync.catalog.dto.CatalogItemResponse;
import com.enterprise.spendsync.catalog.internal.service.CatalogService;
import com.enterprise.spendsync.shared.config.Endpoints;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Endpoints.Catalog.BASE)
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping(Endpoints.Catalog.SEARCH)
    @PreAuthorize("hasAnyAuthority('PERM_PR_CREATE', 'PERM_PR_READ_OWN', 'PERM_PR_READ_ALL', 'PERM_PO_READ', 'PERM_ORG_MANAGE', 'PERM_VENDOR_MANAGE')")
    public ResponseEntity<Page<CatalogItemResponse>> searchItems(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false, defaultValue = "true") Boolean activeOnly,
            @PageableDefault(size = 20, sort = "isPreferred", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return ResponseEntity.ok(catalogService.searchCatalogItems(tenantId, q, categoryId, activeOnly, pageable));
    }

    @GetMapping(Endpoints.Catalog.CATEGORIES)
    @PreAuthorize("hasAnyAuthority('PERM_PR_CREATE', 'PERM_PR_READ_OWN', 'PERM_PR_READ_ALL', 'PERM_PO_READ', 'PERM_ORG_MANAGE', 'PERM_VENDOR_MANAGE')")
    public ResponseEntity<List<CatalogCategoryDto>> getCategories() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return ResponseEntity.ok(catalogService.getCategoryTree(tenantId));
    }

    @GetMapping(Endpoints.Catalog.ITEM_BY_ID)
    @PreAuthorize("hasAnyAuthority('PERM_PR_CREATE', 'PERM_PR_READ_OWN', 'PERM_PR_READ_ALL', 'PERM_PO_READ', 'PERM_ORG_MANAGE', 'PERM_VENDOR_MANAGE')")
    public ResponseEntity<CatalogItemResponse> getItemById(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return ResponseEntity.ok(catalogService.getCatalogItemById(tenantId, id));
    }

    @GetMapping(Endpoints.Catalog.AUTOFILL)
    @PreAuthorize("hasAnyAuthority('PERM_PR_CREATE', 'PERM_PR_READ_OWN', 'PERM_PR_READ_ALL', 'PERM_PO_READ', 'PERM_ORG_MANAGE', 'PERM_VENDOR_MANAGE')")
    public ResponseEntity<CatalogAutofillResponse> getItemAutofill(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return ResponseEntity.ok(catalogService.getCatalogItemAutofill(tenantId, id));
    }

    @GetMapping(Endpoints.Catalog.HEALTH)
    @PreAuthorize("hasAnyAuthority('PERM_PR_READ_ALL', 'PERM_PO_READ', 'PERM_ORG_MANAGE', 'PERM_VENDOR_MANAGE', 'PERM_BUDGET_READ')")
    public ResponseEntity<CatalogHealthMetricsDto> getHealthMetrics() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return ResponseEntity.ok(catalogService.getCatalogHealthMetrics(tenantId));
    }
}
