package com.enterprise.spendsync.catalog.service;

import com.enterprise.spendsync.catalog.dto.CatalogAutofillResponse;
import com.enterprise.spendsync.catalog.dto.CatalogCategoryDto;
import com.enterprise.spendsync.catalog.dto.CatalogHealthMetricsDto;
import com.enterprise.spendsync.catalog.dto.CatalogItemResponse;
import com.enterprise.spendsync.catalog.internal.domain.CatalogCategory;
import com.enterprise.spendsync.catalog.internal.domain.CatalogItem;
import com.enterprise.spendsync.catalog.internal.repository.CatalogCategoryRepository;
import com.enterprise.spendsync.catalog.internal.repository.CatalogItemRepository;
import com.enterprise.spendsync.catalog.internal.service.CatalogServiceImpl;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.purchasing.internal.domain.PaymentTerms;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import com.enterprise.spendsync.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogService Unit & Mock Tests (Search, Autofill, Category Tree & Health)")
class CatalogServiceTest {

    @Mock
    private CatalogItemRepository itemRepository;

    @Mock
    private CatalogCategoryRepository categoryRepository;

    @InjectMocks
    private CatalogServiceImpl catalogService;

    private UUID tenantId;
    private Tenant tenant;
    private CatalogCategory category;
    private Vendor vendor;
    private CatalogItem item;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("SpendSync Global");

        category = new CatalogCategory(tenant, null, "CAT-HW", "Hardware", null, null);
        category.setId(UUID.randomUUID());

        vendor = new Vendor();
        vendor.setId(UUID.randomUUID());
        vendor.setName("Apple Distribution TR");
        vendor.setTaxNumber("1112223334");
        vendor.setOrderEmail("orders@apple-dist.com");
        vendor.setPaymentTerms(PaymentTerms.NET_30);

        item = new CatalogItem(
                tenant,
                "MAC-M3",
                "MacBook Pro 14",
                "Apple M3 Pro Laptop",
                category,
                vendor,
                new BigDecimal("85000.00"),
                "TRY",
                new BigDecimal("0.20"),
                "PIECE",
                "CNT-2026",
                LocalDate.now().minusMonths(1),
                LocalDate.now().plusMonths(6),
                true,
                "GL-150-01",
                null
        );
        item.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should search catalog items returning paginated response")
    void shouldSearchCatalogItems() {
        Pageable pageable = PageRequest.of(0, 10);
        when(itemRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(item), pageable, 1));

        Page<CatalogItemResponse> result = catalogService.searchCatalogItems(tenantId, "MacBook", null, true, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).itemCode()).isEqualTo("MAC-M3");
        assertThat(result.getContent().get(0).unitPrice()).isEqualByComparingTo(new BigDecimal("85000.00"));
    }

    @Test
    @DisplayName("Should get catalog item by ID or throw ResourceNotFoundException")
    void shouldGetCatalogItemById() {
        when(itemRepository.findWithDetailsById(tenantId, item.getId())).thenReturn(Optional.of(item));

        CatalogItemResponse response = catalogService.getCatalogItemById(tenantId, item.getId());

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(item.getId());
        assertThat(response.name()).isEqualTo("MacBook Pro 14");
    }

    @Test
    @DisplayName("Should generate autofill suggestions with suggested vendor and budget hint")
    void shouldGetCatalogItemAutofill() {
        when(itemRepository.findWithDetailsById(tenantId, item.getId())).thenReturn(Optional.of(item));

        CatalogAutofillResponse autofill = catalogService.getCatalogItemAutofill(tenantId, item.getId());

        assertThat(autofill).isNotNull();
        assertThat(autofill.itemCode()).isEqualTo("MAC-M3");
        assertThat(autofill.lineItemSuggestion().unitPrice()).isEqualByComparingTo(new BigDecimal("85000.00"));
        assertThat(autofill.suggestedVendor()).isNotNull();
        assertThat(autofill.suggestedVendor().vendorName()).isEqualTo("Apple Distribution TR");
        assertThat(autofill.budgetHint().glAccountCode()).isEqualTo("GL-150-01");
    }

    @Test
    @DisplayName("Should build hierarchical category tree")
    void shouldGetCategoryTree() {
        CatalogCategory root = new CatalogCategory(tenant, null, "CAT-IT", "IT", null, null);
        root.setId(UUID.randomUUID());
        CatalogCategory child = new CatalogCategory(tenant, root, "CAT-LAPTOP", "Laptops", null, null);
        child.setId(UUID.randomUUID());
        root.getChildren().add(child);

        when(categoryRepository.findByTenantIdAndParentIsNull(tenantId)).thenReturn(List.of(root));
        when(itemRepository.countByTenantIdAndCategory(eq(tenantId), any(CatalogCategory.class))).thenReturn(5L);

        List<CatalogCategoryDto> tree = catalogService.getCategoryTree(tenantId);

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).name()).isEqualTo("IT");
        assertThat(tree.get(0).children()).hasSize(1);
        assertThat(tree.get(0).children().get(0).name()).isEqualTo("Laptops");
    }

    @Test
    @DisplayName("Should return catalog health metrics")
    void shouldGetCatalogHealthMetrics() {
        when(itemRepository.countByTenantIdAndIsActiveTrue(tenantId)).thenReturn(150L);
        when(categoryRepository.countByTenantId(tenantId)).thenReturn(12L);
        when(itemRepository.countExpiringSoon(eq(tenantId), any(LocalDate.class), any(LocalDate.class))).thenReturn(10L);
        when(itemRepository.countExpired(eq(tenantId), any(LocalDate.class))).thenReturn(5L);
        when(itemRepository.countByTenantIdAndIsPreferredTrueAndIsActiveTrue(tenantId)).thenReturn(80L);
        when(itemRepository.findTop10ByTenantIdAndIsPreferredTrueAndIsActiveTrueOrderByUpdatedAtDesc(tenantId)).thenReturn(List.of(item));

        CatalogHealthMetricsDto metrics = catalogService.getCatalogHealthMetrics(tenantId);

        assertThat(metrics).isNotNull();
        assertThat(metrics.totalActiveItems()).isEqualTo(150L);
        assertThat(metrics.totalCategories()).isEqualTo(12L);
        assertThat(metrics.preferredItemsCount()).isEqualTo(80L);
        assertThat(metrics.expiredItemsCount()).isEqualTo(5L);
        assertThat(metrics.expiringIn30DaysCount()).isEqualTo(10L);
    }
}
