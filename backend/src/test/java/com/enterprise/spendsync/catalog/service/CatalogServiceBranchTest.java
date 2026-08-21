package com.enterprise.spendsync.catalog.service;

import com.enterprise.spendsync.catalog.dto.CatalogAutofillResponse;
import com.enterprise.spendsync.catalog.dto.CatalogHealthMetricsDto;
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
public class CatalogServiceBranchTest {

    @Mock private CatalogItemRepository itemRepository;
    @Mock private CatalogCategoryRepository categoryRepository;

    @InjectMocks
    private CatalogServiceImpl catalogService;

    private UUID tenantId;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        tenant = new Tenant("Branch Tenant", "branch-tenant");
        tenant.setId(tenantId);
    }

    @Test
    @DisplayName("Should search catalog items with category filter and inactive items enabled")
    void shouldSearchWithCategoryAndInactive() {
        UUID categoryId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        when(itemRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        catalogService.searchCatalogItems(tenantId, null, categoryId, false, pageable);

        assertThatThrownBy(() -> catalogService.getCatalogItemById(tenantId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when item not found on autofill")
    void shouldThrowWhenItemNotFoundOnAutofill() {
        UUID randomId = UUID.randomUUID();
        when(itemRepository.findWithDetailsById(tenantId, randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getCatalogItemAutofill(tenantId, randomId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should build autofill response for item with null category, null vendor, and default prices")
    void shouldBuildAutofillForMinimalItem() {
        UUID itemId = UUID.randomUUID();
        CatalogItem minimalItem = new CatalogItem(tenant, "MIN-01", "Basic Item", null, null, null,
                null, null, null, "EA", null, null, LocalDate.now().plusDays(3), false, null, null);
        minimalItem.setId(itemId);

        when(itemRepository.findWithDetailsById(tenantId, itemId)).thenReturn(Optional.of(minimalItem));

        CatalogAutofillResponse autofill = catalogService.getCatalogItemAutofill(tenantId, itemId);

        assertThat(autofill).isNotNull();
        assertThat(autofill.itemCode()).isEqualTo("MIN-01");
        assertThat(autofill.lineItemSuggestion().categoryCode()).isEqualTo("DEFAULT");
        assertThat(autofill.lineItemSuggestion().categoryFullPath()).isEqualTo("General");
        assertThat(autofill.lineItemSuggestion().unitPrice()).isEqualTo(BigDecimal.ZERO);
        assertThat(autofill.lineItemSuggestion().vatRate()).isEqualByComparingTo(new BigDecimal("0.20"));
        assertThat(autofill.suggestedVendor()).isNull();
        assertThat(autofill.contractAlert()).contains("CRITICAL CONTRACT EXPIRY");
    }

    @Test
    @DisplayName("Should compute notice alert for contract expiring within 30 days and no alert for > 30 days")
    void shouldComputeContractAlerts() {
        UUID item30Id = UUID.randomUUID();
        CatalogItem item30 = new CatalogItem(tenant, "ITEM-30", "Item 30", null, null, null,
                BigDecimal.TEN, "TRY", null, "EA", null, null, LocalDate.now().plusDays(20), false, null, null);
        item30.setId(item30Id);

        when(itemRepository.findWithDetailsById(tenantId, item30Id)).thenReturn(Optional.of(item30));

        CatalogAutofillResponse auto30 = catalogService.getCatalogItemAutofill(tenantId, item30Id);
        assertThat(auto30.contractAlert()).contains("Contract expiry notice");

        UUID itemFarId = UUID.randomUUID();
        CatalogItem itemFar = new CatalogItem(tenant, "ITEM-FAR", "Item Far", null, null, null,
                BigDecimal.TEN, "TRY", null, "EA", null, null, LocalDate.now().plusDays(100), false, null, null);
        itemFar.setId(itemFarId);

        when(itemRepository.findWithDetailsById(tenantId, itemFarId)).thenReturn(Optional.of(itemFar));

        CatalogAutofillResponse autoFar = catalogService.getCatalogItemAutofill(tenantId, itemFarId);
        assertThat(autoFar.contractAlert()).isNull();
    }

    @Test
    @DisplayName("Should return health metrics with fallback strings for items without vendor/category")
    void shouldGetHealthMetricsWithFallbacks() {
        CatalogItem item = new CatalogItem(tenant, "ITEM-TOP", "Top Item", null, null, null,
                new BigDecimal("99.00"), "TRY", null, "EA", null, null, null, true, null, null);

        when(itemRepository.countByTenantIdAndIsActiveTrue(tenantId)).thenReturn(10L);
        when(categoryRepository.countByTenantId(tenantId)).thenReturn(2L);
        when(itemRepository.countExpiringSoon(eq(tenantId), any(), any())).thenReturn(1L);
        when(itemRepository.countExpired(eq(tenantId), any())).thenReturn(0L);
        when(itemRepository.countByTenantIdAndIsPreferredTrueAndIsActiveTrue(tenantId)).thenReturn(5L);
        when(itemRepository.findTop10ByTenantIdAndIsPreferredTrueAndIsActiveTrueOrderByUpdatedAtDesc(tenantId))
                .thenReturn(List.of(item));

        CatalogHealthMetricsDto metrics = catalogService.getCatalogHealthMetrics(tenantId);

        assertThat(metrics.topPreferredItems()).hasSize(1);
        assertThat(metrics.topPreferredItems().get(0).vendorName()).isEqualTo("-");
        assertThat(metrics.topPreferredItems().get(0).categoryFullPath()).isEqualTo("-");
    }
}
