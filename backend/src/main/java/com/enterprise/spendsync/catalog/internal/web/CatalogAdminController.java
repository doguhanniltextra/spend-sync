package com.enterprise.spendsync.catalog.internal.web;

import com.enterprise.spendsync.catalog.dto.CatalogCategoryCreateRequest;
import com.enterprise.spendsync.catalog.dto.CatalogCategoryDto;
import com.enterprise.spendsync.catalog.dto.CatalogItemCreateRequest;
import com.enterprise.spendsync.catalog.dto.CatalogItemResponse;
import com.enterprise.spendsync.catalog.dto.CatalogItemUpdateRequest;
import com.enterprise.spendsync.catalog.dto.CsvImportResultDto;
import com.enterprise.spendsync.catalog.internal.service.CatalogAdminService;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.shared.config.Endpoints;
import com.enterprise.spendsync.shared.security.UserPrincipal;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping(Endpoints.AdminCatalog.BASE)
public class CatalogAdminController {

    private final CatalogAdminService catalogAdminService;
    private final UserRepository userRepository;

    public CatalogAdminController(CatalogAdminService catalogAdminService,
                                  UserRepository userRepository) {
        this.catalogAdminService = catalogAdminService;
        this.userRepository = userRepository;
    }

    @PostMapping(Endpoints.AdminCatalog.ITEMS)
    @PreAuthorize("hasAnyAuthority('PERM_VENDOR_MANAGE', 'PERM_ORG_MANAGE', 'PERM_PO_CREATE', 'PERM_PO_UPDATE')")
    public ResponseEntity<CatalogItemResponse> createItem(@Valid @RequestBody CatalogItemCreateRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        User currentUser = resolveCurrentUser(tenantId);
        return ResponseEntity.ok(catalogAdminService.createCatalogItem(tenantId, request, currentUser));
    }

    @PutMapping(Endpoints.AdminCatalog.ITEM_BY_ID)
    @PreAuthorize("hasAnyAuthority('PERM_VENDOR_MANAGE', 'PERM_ORG_MANAGE', 'PERM_PO_CREATE', 'PERM_PO_UPDATE')")
    public ResponseEntity<CatalogItemResponse> updateItem(
            @PathVariable UUID id,
            @Valid @RequestBody CatalogItemUpdateRequest request
    ) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return ResponseEntity.ok(catalogAdminService.updateCatalogItem(tenantId, id, request));
    }

    @DeleteMapping(Endpoints.AdminCatalog.ITEM_BY_ID)
    @PreAuthorize("hasAnyAuthority('PERM_VENDOR_MANAGE', 'PERM_ORG_MANAGE', 'PERM_PO_CREATE', 'PERM_PO_UPDATE')")
    public ResponseEntity<Void> deleteItem(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        catalogAdminService.deleteCatalogItem(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(Endpoints.AdminCatalog.CATEGORIES)
    @PreAuthorize("hasAnyAuthority('PERM_VENDOR_MANAGE', 'PERM_ORG_MANAGE', 'PERM_PO_CREATE', 'PERM_PO_UPDATE')")
    public ResponseEntity<CatalogCategoryDto> createCategory(@Valid @RequestBody CatalogCategoryCreateRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return ResponseEntity.ok(catalogAdminService.createCategory(tenantId, request));
    }

    @PostMapping(value = Endpoints.AdminCatalog.IMPORT, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('PERM_VENDOR_MANAGE', 'PERM_ORG_MANAGE', 'PERM_PO_CREATE', 'PERM_PO_UPDATE')")
    public ResponseEntity<CsvImportResultDto> importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        UUID tenantId = TenantContext.getRequiredTenantId();
        User currentUser = resolveCurrentUser(tenantId);
        return ResponseEntity.ok(catalogAdminService.importCatalogFromCsv(tenantId, file.getInputStream(), currentUser));
    }

    @GetMapping(value = Endpoints.AdminCatalog.EXPORT, produces = "text/csv")
    @PreAuthorize("hasAnyAuthority('PERM_VENDOR_MANAGE', 'PERM_ORG_MANAGE', 'PERM_PO_CREATE', 'PERM_PO_UPDATE')")
    public ResponseEntity<byte[]> exportCsv() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        byte[] csvData = catalogAdminService.exportCatalogToCsv(tenantId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"catalog_export.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    private User resolveCurrentUser(UUID tenantId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return userRepository.findByIdAndTenantId(principal.getId(), tenantId).orElse(null);
        }
        return null;
    }
}
