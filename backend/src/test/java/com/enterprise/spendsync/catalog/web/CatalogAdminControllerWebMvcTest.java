package com.enterprise.spendsync.catalog.web;

import com.enterprise.spendsync.catalog.dto.*;
import com.enterprise.spendsync.catalog.internal.service.CatalogAdminService;
import com.enterprise.spendsync.catalog.internal.web.CatalogAdminController;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.shared.config.Endpoints;
import com.enterprise.spendsync.shared.security.UserPrincipal;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogAdminController REST Web API Slice Tests")
class CatalogAdminControllerWebMvcTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private CatalogAdminService catalogAdminService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CatalogAdminController catalogAdminController;

    private UUID tenantId;
    private UUID itemId;
    private User adminUser;
    private CatalogItemResponse sampleItemResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(catalogAdminController)
                .setMessageConverters(
                        new org.springframework.http.converter.ByteArrayHttpMessageConverter(),
                        new org.springframework.http.converter.StringHttpMessageConverter(),
                        new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper)
                )
                .build();
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        itemId = UUID.randomUUID();
        adminUser = new User("admin@spendsync.com", "pass", "Admin", "User", null, "TR");
        adminUser.setId(UUID.randomUUID());

        sampleItemResponse = new CatalogItemResponse(
                itemId,
                "A4-PAPER",
                "A4 Copy Paper",
                "Box of 5 reams",
                UUID.randomUUID(),
                "Office Supplies",
                "Office Supplies",
                UUID.randomUUID(),
                "Office Vendor TR",
                "9998887776",
                "TIER_1",
                new BigDecimal("450.00"),
                "TRY",
                new BigDecimal("0.20"),
                "BOX",
                "CNT-2026",
                LocalDate.now(),
                LocalDate.now().plusYears(1),
                true,
                true,
                "GL-770-01",
                null,
                Instant.now(),
                Instant.now()
        );

        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), tenantId, null, "ADMIN", adminUser.getEmail(), null, "Admin User", true, Set.of(), Set.of()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, "token", Set.of()));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/v1/admin/catalog/items - creates catalog item")
    void shouldCreateItem() throws Exception {
        CatalogItemCreateRequest request = new CatalogItemCreateRequest(
                "A4-PAPER", "A4 Copy Paper", "Box of 5 reams", UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("450.00"), "TRY", new BigDecimal("0.20"), "BOX", "CNT-2026",
                LocalDate.now(), LocalDate.now().plusYears(1), true, "GL-770-01"
        );

        when(userRepository.findByIdAndTenantId(adminUser.getId(), tenantId)).thenReturn(Optional.of(adminUser));
        when(catalogAdminService.createCatalogItem(eq(tenantId), any(CatalogItemCreateRequest.class), eq(adminUser)))
                .thenReturn(sampleItemResponse);

        mockMvc.perform(post(Endpoints.AdminCatalog.BASE + Endpoints.AdminCatalog.ITEMS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCode").value("A4-PAPER"))
                .andExpect(jsonPath("$.unitPrice").value(450.00));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/catalog/items/{id} - updates catalog item")
    void shouldUpdateItem() throws Exception {
        CatalogItemUpdateRequest request = new CatalogItemUpdateRequest(
                "A4 Copy Paper Premium", "Updated description", UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("480.00"), "TRY", new BigDecimal("0.20"), "BOX", "CNT-2026",
                LocalDate.now(), LocalDate.now().plusYears(1), true, true, "GL-770-02"
        );

        when(catalogAdminService.updateCatalogItem(eq(tenantId), eq(itemId), any(CatalogItemUpdateRequest.class)))
                .thenReturn(sampleItemResponse);

        mockMvc.perform(put(Endpoints.AdminCatalog.BASE + "/items/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCode").value("A4-PAPER"));
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/catalog/items/{id} - soft-deletes item returning 204 No Content")
    void shouldDeleteItem() throws Exception {
        mockMvc.perform(delete(Endpoints.AdminCatalog.BASE + "/items/" + itemId))
                .andExpect(status().isNoContent());

        verify(catalogAdminService).deleteCatalogItem(tenantId, itemId);
    }

    @Test
    @DisplayName("POST /api/v1/admin/catalog/categories - creates category")
    void shouldCreateCategory() throws Exception {
        CatalogCategoryCreateRequest request = new CatalogCategoryCreateRequest(null, "CAT-PAPER", "Paper", null, null);
        CatalogCategoryDto responseDto = new CatalogCategoryDto(UUID.randomUUID(), "CAT-PAPER", "Paper", "Paper", null, null, null, 0L, List.of());

        when(catalogAdminService.createCategory(eq(tenantId), any(CatalogCategoryCreateRequest.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post(Endpoints.AdminCatalog.BASE + Endpoints.AdminCatalog.CATEGORIES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CAT-PAPER"))
                .andExpect(jsonPath("$.name").value("Paper"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/catalog/import - imports items from uploaded CSV file")
    void shouldImportCsv() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "catalog.csv", "text/csv", "item_code,name\nIT-01,Laptop\n".getBytes(StandardCharsets.UTF_8)
        );

        CsvImportResultDto importResult = new CsvImportResultDto(1, 1, 0, List.of());
        when(userRepository.findByIdAndTenantId(adminUser.getId(), tenantId)).thenReturn(Optional.of(adminUser));
        when(catalogAdminService.importCatalogFromCsv(eq(tenantId), any(InputStream.class), eq(adminUser)))
                .thenReturn(importResult);

        mockMvc.perform(multipart(Endpoints.AdminCatalog.BASE + Endpoints.AdminCatalog.IMPORT)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(1))
                .andExpect(jsonPath("$.successCount").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/admin/catalog/export - exports catalog to CSV file")
    void shouldExportCsv() throws Exception {
        byte[] csvBytes = "item_code,name\nIT-01,Laptop\n".getBytes(StandardCharsets.UTF_8);
        when(catalogAdminService.exportCatalogToCsv(tenantId)).thenReturn(csvBytes);

        mockMvc.perform(get(Endpoints.AdminCatalog.BASE + Endpoints.AdminCatalog.EXPORT))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"catalog_export.csv\""))
                .andExpect(content().contentType("text/csv; charset=UTF-8"));
    }
}
