package com.enterprise.spendsync.catalog.web;

import com.enterprise.spendsync.catalog.dto.CatalogAutofillResponse;
import com.enterprise.spendsync.catalog.dto.CatalogCategoryDto;
import com.enterprise.spendsync.catalog.dto.CatalogHealthMetricsDto;
import com.enterprise.spendsync.catalog.dto.CatalogItemResponse;
import com.enterprise.spendsync.catalog.internal.service.CatalogService;
import com.enterprise.spendsync.catalog.internal.web.CatalogController;
import com.enterprise.spendsync.shared.config.Endpoints;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogController REST Web API Slice Tests")
class CatalogControllerWebMvcTest {

    private MockMvc mockMvc;

    @Mock
    private CatalogService catalogService;

    @InjectMocks
    private CatalogController catalogController;

    private UUID tenantId;
    private UUID itemId;
    private CatalogItemResponse sampleItemResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(catalogController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(
                        new org.springframework.http.converter.ByteArrayHttpMessageConverter(),
                        new org.springframework.http.converter.StringHttpMessageConverter(),
                        new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(
                                new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules())
                )
                .build();
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        itemId = UUID.randomUUID();
        sampleItemResponse = new CatalogItemResponse(
                itemId,
                "MAC-M3",
                "MacBook Pro 14",
                "Apple M3 Pro Laptop",
                UUID.randomUUID(),
                "Hardware",
                "IT > Hardware",
                UUID.randomUUID(),
                "Apple Distribution TR",
                "1112223334",
                "TIER_1",
                new BigDecimal("85000.00"),
                "TRY",
                new BigDecimal("0.20"),
                "PIECE",
                "CNT-2026",
                LocalDate.now(),
                LocalDate.now().plusMonths(6),
                true,
                true,
                "GL-150-01",
                null,
                Instant.now(),
                Instant.now()
        );
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("GET /api/v1/catalog/search - returns paginated catalog items")
    void shouldSearchItems() throws Exception {
        when(catalogService.searchCatalogItems(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleItemResponse), org.springframework.data.domain.PageRequest.of(0, 20), 1));

        mockMvc.perform(get(Endpoints.Catalog.BASE + Endpoints.Catalog.SEARCH)
                        .param("q", "MacBook"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].itemCode").value("MAC-M3"))
                .andExpect(jsonPath("$.content[0].unitPrice").value(85000.00));
    }

    @Test
    @DisplayName("GET /api/v1/catalog/categories - returns category hierarchy tree")
    void shouldGetCategories() throws Exception {
        CatalogCategoryDto cat = new CatalogCategoryDto(UUID.randomUUID(), "CAT-HW", "Hardware", "Hardware", null, null, null, 10L, List.of());
        when(catalogService.getCategoryTree(tenantId)).thenReturn(List.of(cat));

        mockMvc.perform(get(Endpoints.Catalog.BASE + Endpoints.Catalog.CATEGORIES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Hardware"))
                .andExpect(jsonPath("$[0].code").value("CAT-HW"));
    }

    @Test
    @DisplayName("GET /api/v1/catalog/items/{id} - returns single catalog item details")
    void shouldGetItemById() throws Exception {
        when(catalogService.getCatalogItemById(tenantId, itemId)).thenReturn(sampleItemResponse);

        mockMvc.perform(get(Endpoints.Catalog.BASE + "/items/" + itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId.toString()))
                .andExpect(jsonPath("$.itemCode").value("MAC-M3"));
    }

    @Test
    @DisplayName("GET /api/v1/catalog/items/{id}/autofill - returns PR line item autofill suggestions")
    void shouldGetItemAutofill() throws Exception {
        CatalogAutofillResponse autofill = new CatalogAutofillResponse(
                itemId,
                "MAC-M3",
                "MacBook Pro 14",
                new CatalogAutofillResponse.LineItemSuggestion("MacBook Pro 14", "CAT-HW", "Hardware", BigDecimal.ONE, "PIECE", new BigDecimal("85000.00"), new BigDecimal("0.20"), new BigDecimal("85000.00")),
                new CatalogAutofillResponse.SuggestedVendor(UUID.randomUUID(), "Apple TR", "1112223334", "orders@apple.com", "NET_30"),
                new CatalogAutofillResponse.BudgetHint("GL-150-01", null),
                null
        );

        when(catalogService.getCatalogItemAutofill(tenantId, itemId)).thenReturn(autofill);

        mockMvc.perform(get(Endpoints.Catalog.BASE + "/items/" + itemId + "/autofill"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCode").value("MAC-M3"))
                .andExpect(jsonPath("$.suggestedVendor.vendorName").value("Apple TR"));
    }

    @Test
    @DisplayName("GET /api/v1/catalog/health - returns health and expiring contract metrics")
    void shouldGetHealthMetrics() throws Exception {
        CatalogHealthMetricsDto health = new CatalogHealthMetricsDto(100L, 10L, 12L, 4L, 5L, 80L, List.of());
        when(catalogService.getCatalogHealthMetrics(tenantId)).thenReturn(health);

        mockMvc.perform(get(Endpoints.Catalog.BASE + Endpoints.Catalog.HEALTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActiveItems").value(100))
                .andExpect(jsonPath("$.expiredItemsCount").value(5));
    }
}
