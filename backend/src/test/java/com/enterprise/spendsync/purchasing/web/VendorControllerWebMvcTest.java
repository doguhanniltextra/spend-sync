package com.enterprise.spendsync.purchasing.web;

import com.enterprise.spendsync.purchasing.internal.domain.PaymentTerms;
import com.enterprise.spendsync.purchasing.internal.domain.VendorCategory;
import com.enterprise.spendsync.purchasing.internal.domain.VendorStatus;
import com.enterprise.spendsync.purchasing.internal.domain.VendorTier;
import com.enterprise.spendsync.purchasing.internal.dto.CreateVendorRequest;
import com.enterprise.spendsync.purchasing.internal.dto.UpdateVendorStatusRequest;
import com.enterprise.spendsync.purchasing.internal.dto.VendorResponse;
import com.enterprise.spendsync.purchasing.internal.service.VendorService;
import com.enterprise.spendsync.purchasing.internal.web.VendorController;
import com.enterprise.spendsync.shared.config.Endpoints;
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
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VendorController REST Web API Slice Tests")
class VendorControllerWebMvcTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private VendorService vendorService;

    @InjectMocks
    private VendorController vendorController;

    private UUID tenantId;
    private UUID vendorId;
    private VendorResponse sampleVendorResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(vendorController)
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new StringHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper)
                )
                .build();

        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        vendorId = UUID.randomUUID();
        sampleVendorResponse = new VendorResponse(
                vendorId,
                "Acme Global Tech",
                "1234567890",
                "Besiktas",
                VendorCategory.IT_HARDWARE,
                VendorTier.TIER_1_STRATEGIC,
                true,
                "orders@acmeglobal.com",
                "+90 212 555 0100",
                "Buyukdere Cad. No: 12",
                "Istanbul",
                "TR",
                PaymentTerms.NET_30,
                "Garanti BBVA",
                "TR330006200000001234567890",
                VendorStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("POST /api/v1/purchasing/vendors - creates vendor returning 201 Created")
    void shouldCreateVendor() throws Exception {
        CreateVendorRequest request = new CreateVendorRequest(
                "Acme Global Tech",
                "1234567890",
                "Besiktas",
                VendorCategory.IT_HARDWARE,
                VendorTier.TIER_1_STRATEGIC,
                true,
                "orders@acmeglobal.com",
                "+90 212 555 0100",
                "Buyukdere Cad. No: 12",
                "Istanbul",
                "TR",
                PaymentTerms.NET_30,
                "Garanti BBVA",
                "TR330006200000001234567890"
        );

        when(vendorService.createVendor(any(CreateVendorRequest.class))).thenReturn(sampleVendorResponse);

        mockMvc.perform(post(Endpoints.Purchasing.VENDORS_BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(vendorId.toString()))
                .andExpect(jsonPath("$.name").value("Acme Global Tech"))
                .andExpect(jsonPath("$.taxNumber").value("1234567890"));
    }

    @Test
    @DisplayName("GET /api/v1/purchasing/vendors/{id} - returns vendor details")
    void shouldGetVendorById() throws Exception {
        when(vendorService.getVendorById(vendorId)).thenReturn(sampleVendorResponse);

        mockMvc.perform(get(Endpoints.Purchasing.VENDORS_BASE + "/" + vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vendorId.toString()))
                .andExpect(jsonPath("$.name").value("Acme Global Tech"));
    }

    @Test
    @DisplayName("GET /api/v1/purchasing/vendors - returns list of vendors with filters")
    void shouldGetAllVendors() throws Exception {
        when(vendorService.getAllVendors(eq(VendorStatus.ACTIVE), any(), any()))
                .thenReturn(List.of(sampleVendorResponse));

        mockMvc.perform(get(Endpoints.Purchasing.VENDORS_BASE)
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Acme Global Tech"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("PATCH /api/v1/purchasing/vendors/{id}/status - updates vendor status")
    void shouldUpdateVendorStatus() throws Exception {
        UpdateVendorStatusRequest request = new UpdateVendorStatusRequest(VendorStatus.BLOCKED);
        VendorResponse updatedResponse = new VendorResponse(
                vendorId, "Acme Global Tech", "1234567890", "Besiktas", VendorCategory.IT_HARDWARE,
                VendorTier.TIER_1_STRATEGIC, true, "orders@acmeglobal.com", null, null, null, "TR",
                PaymentTerms.NET_30, null, null, VendorStatus.BLOCKED, Instant.now(), Instant.now()
        );

        when(vendorService.updateVendorStatus(eq(vendorId), any(UpdateVendorStatusRequest.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(patch(Endpoints.Purchasing.VENDORS_BASE + "/" + vendorId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }
}
