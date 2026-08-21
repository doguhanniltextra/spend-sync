package com.enterprise.spendsync.catalog.service;

import com.enterprise.spendsync.catalog.dto.CatalogCategoryCreateRequest;
import com.enterprise.spendsync.catalog.dto.CatalogCategoryDto;
import com.enterprise.spendsync.catalog.dto.CatalogItemCreateRequest;
import com.enterprise.spendsync.catalog.dto.CatalogItemResponse;
import com.enterprise.spendsync.catalog.dto.CatalogItemUpdateRequest;
import com.enterprise.spendsync.catalog.dto.CsvImportResultDto;
import com.enterprise.spendsync.catalog.internal.domain.CatalogCategory;
import com.enterprise.spendsync.catalog.internal.domain.CatalogItem;
import com.enterprise.spendsync.catalog.internal.repository.CatalogCategoryRepository;
import com.enterprise.spendsync.catalog.internal.repository.CatalogItemRepository;
import com.enterprise.spendsync.catalog.internal.service.CatalogAdminServiceImpl;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import com.enterprise.spendsync.purchasing.internal.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogAdminService Unit & Mock Tests (Item Master CRUD, Categories & CSV)")
class CatalogAdminServiceTest {

    @Mock
    private CatalogItemRepository itemRepository;

    @Mock
    private CatalogCategoryRepository categoryRepository;

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private CatalogAdminServiceImpl catalogAdminService;

    private UUID tenantId;
    private Tenant tenant;
    private CatalogCategory category;
    private Vendor vendor;
    private User adminUser;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("SpendSync Global");

        category = new CatalogCategory(tenant, null, "CAT-OFFICE", "Office Supplies", null, null);
        category.setId(UUID.randomUUID());

        vendor = new Vendor();
        vendor.setId(UUID.randomUUID());
        vendor.setName("Office Supply TR");

        adminUser = new User("admin@spendsync.com", "pass", "Admin", "User", null, "TR");
        adminUser.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should create catalog item successfully and map to response")
    void shouldCreateCatalogItemSuccessfully() {
        CatalogItemCreateRequest request = new CatalogItemCreateRequest(
                "A4-PAPER",
                "A4 Copy Paper 80g",
                "Box of 5 reams",
                category.getId(),
                vendor.getId(),
                new BigDecimal("450.00"),
                "TRY",
                new BigDecimal("0.20"),
                "BOX",
                "CNT-PAPER-2026",
                LocalDate.now(),
                LocalDate.now().plusYears(1),
                true,
                "GL-770-01"
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(itemRepository.findByTenantIdAndItemCode(tenantId, "A4-PAPER")).thenReturn(Optional.empty());
        when(categoryRepository.findByTenantIdAndId(tenantId, category.getId())).thenReturn(Optional.of(category));
        when(vendorRepository.findByIdAndTenantId(vendor.getId(), tenantId)).thenReturn(Optional.of(vendor));

        when(itemRepository.save(any(CatalogItem.class))).thenAnswer(i -> {
            CatalogItem itm = i.getArgument(0);
            itm.setId(UUID.randomUUID());
            return itm;
        });

        CatalogItemResponse response = catalogAdminService.createCatalogItem(tenantId, request, adminUser);

        assertThat(response).isNotNull();
        assertThat(response.itemCode()).isEqualTo("A4-PAPER");
        assertThat(response.name()).isEqualTo("A4 Copy Paper 80g");
        assertThat(response.unitPrice()).isEqualByComparingTo(new BigDecimal("450.00"));
        assertThat(response.isPreferred()).isTrue();

        verify(itemRepository).save(any(CatalogItem.class));
    }

    @Test
    @DisplayName("Should reject item creation when duplicate itemCode already exists in tenant")
    void shouldRejectDuplicateItemCode() {
        CatalogItemCreateRequest request = new CatalogItemCreateRequest(
                "A4-PAPER", "A4 Paper", "Desc", null, null, new BigDecimal("450.00"), "TRY",
                new BigDecimal("0.20"), "BOX", null, null, null, false, null
        );

        CatalogItem existing = new CatalogItem(tenant, "A4-PAPER", "Old Paper", "Old Desc", null, null,
                new BigDecimal("400.00"), "TRY", new BigDecimal("0.20"), "BOX", null, null, null, false, null, null);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(itemRepository.findByTenantIdAndItemCode(tenantId, "A4-PAPER")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> catalogAdminService.createCatalogItem(tenantId, request, adminUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Item with code 'A4-PAPER' already exists");
    }

    @Test
    @DisplayName("Should update catalog item properties")
    void shouldUpdateCatalogItem() {
        UUID itemId = UUID.randomUUID();
        CatalogItem item = new CatalogItem(tenant, "A4-PAPER", "A4 Paper", "Desc", category, vendor,
                new BigDecimal("450.00"), "TRY", new BigDecimal("0.20"), "BOX", null, null, null, false, null, null);
        item.setId(itemId);

        when(itemRepository.findByTenantIdAndId(tenantId, itemId)).thenReturn(Optional.of(item));
        when(categoryRepository.findByTenantIdAndId(tenantId, category.getId())).thenReturn(Optional.of(category));
        when(vendorRepository.findByIdAndTenantId(vendor.getId(), tenantId)).thenReturn(Optional.of(vendor));
        when(itemRepository.save(any(CatalogItem.class))).thenAnswer(i -> i.getArgument(0));

        CatalogItemUpdateRequest updateRequest = new CatalogItemUpdateRequest(
                "A4 Copy Paper 80g Premium", "Updated description", category.getId(), vendor.getId(),
                new BigDecimal("480.00"), "TRY", new BigDecimal("0.20"), "BOX", "CNT-2026",
                LocalDate.now(), LocalDate.now().plusMonths(12), true, true, "GL-770-02"
        );

        CatalogItemResponse updated = catalogAdminService.updateCatalogItem(tenantId, itemId, updateRequest);

        assertThat(updated.name()).isEqualTo("A4 Copy Paper 80g Premium");
        assertThat(updated.unitPrice()).isEqualByComparingTo(new BigDecimal("480.00"));
        assertThat(updated.isPreferred()).isTrue();
    }

    @Test
    @DisplayName("Should soft-delete catalog item by setting isActive to false")
    void shouldDeleteCatalogItem() {
        UUID itemId = UUID.randomUUID();
        CatalogItem item = new CatalogItem(tenant, "A4-PAPER", "A4 Paper", "Desc", category, vendor,
                new BigDecimal("450.00"), "TRY", new BigDecimal("0.20"), "BOX", null, null, null, false, null, null);
        item.setId(itemId);

        when(itemRepository.findByTenantIdAndId(tenantId, itemId)).thenReturn(Optional.of(item));

        catalogAdminService.deleteCatalogItem(tenantId, itemId);

        assertThat(item.isActive()).isFalse();
        verify(itemRepository).save(item);
    }

    @Test
    @DisplayName("Should create nested category with hierarchical full path")
    void shouldCreateNestedCategory() {
        CatalogCategory root = new CatalogCategory(tenant, null, "CAT-OFFICE", "Office", null, null);
        root.setId(UUID.randomUUID());

        CatalogCategoryCreateRequest request = new CatalogCategoryCreateRequest(root.getId(), "CAT-PAPER", "Paper Supplies", null, null);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(categoryRepository.findByTenantIdAndId(tenantId, root.getId())).thenReturn(Optional.of(root));

        when(categoryRepository.save(any(CatalogCategory.class))).thenAnswer(i -> {
            CatalogCategory cat = i.getArgument(0);
            cat.setId(UUID.randomUUID());
            return cat;
        });

        CatalogCategoryDto response = catalogAdminService.createCategory(tenantId, request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Paper Supplies");
        assertThat(response.fullPath()).isEqualTo("Office / Paper Supplies");
    }

    @Test
    @DisplayName("Should import catalog items from CSV input stream")
    void shouldImportCatalogFromCsv() {
        String csvContent = "item_code,name,description,category_code,vendor_tax_number,unit_price,currency,vat_rate,unit_of_measure,contract_reference,valid_from,valid_until,is_preferred,gl_account_code\n" +
                "PAPER-01,Copy Paper,A4 80g,CAT-OFFICE,,100.00,TRY,0.20,BOX,,,true,\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(itemRepository.findByTenantIdAndItemCode(eq(tenantId), any())).thenReturn(Optional.empty());
        when(categoryRepository.findByTenantIdAndFullPath(tenantId, "CAT-OFFICE")).thenReturn(Optional.of(category));

        when(itemRepository.save(any(CatalogItem.class))).thenAnswer(i -> {
            CatalogItem item = i.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });

        CsvImportResultDto result = catalogAdminService.importCatalogFromCsv(tenantId, inputStream, adminUser);

        assertThat(result).isNotNull();
        assertThat(result.totalRows()).isEqualTo(1);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(0);
    }
}
