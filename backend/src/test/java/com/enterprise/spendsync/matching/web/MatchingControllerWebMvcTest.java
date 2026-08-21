package com.enterprise.spendsync.matching.web;

import com.enterprise.spendsync.matching.internal.domain.InvoiceMatchStatus;
import com.enterprise.spendsync.matching.internal.domain.InvoiceProfile;
import com.enterprise.spendsync.matching.internal.domain.InvoiceStatus;
import com.enterprise.spendsync.matching.internal.domain.InvoiceType;
import com.enterprise.spendsync.matching.internal.dto.*;
import com.enterprise.spendsync.matching.internal.service.MatchingService;
import com.enterprise.spendsync.matching.internal.web.MatchingController;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchingController REST Web API Slice Tests")
class MatchingControllerWebMvcTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private MatchingService matchingService;

    @InjectMocks
    private MatchingController matchingController;

    private UUID tenantId;
    private UUID invoiceId;
    private UUID poId;
    private SupplierInvoiceResponse sampleInvoiceResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(matchingController)
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new StringHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper)
                )
                .build();

        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        invoiceId = UUID.randomUUID();
        poId = UUID.randomUUID();

        sampleInvoiceResponse = new SupplierInvoiceResponse(
                invoiceId,
                "INV-2026-001",
                "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                LocalDate.now(),
                InvoiceType.SATIS,
                InvoiceProfile.TICARI_FATURA,
                poId,
                "PO-2026-00001",
                UUID.randomUUID(),
                "Global Server Supplies",
                "1234567890",
                UUID.randomUUID(),
                "SpendSync Turkey",
                UUID.randomUUID(),
                "Engineering",
                "TRY",
                new BigDecimal("100000.00"),
                new BigDecimal("20000.00"),
                new BigDecimal("120000.00"),
                InvoiceMatchStatus.AUTO_MATCHED,
                InvoiceStatus.APPROVED_FOR_PAYMENT,
                null,
                null,
                null,
                List.of(),
                Instant.now()
        );
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("POST /api/v1/matching/invoices - creates and matches invoice returning 201 Created")
    void shouldCreateAndEvaluateInvoice() throws Exception {
        CreateSupplierInvoiceRequest request = new CreateSupplierInvoiceRequest(
                poId,
                "INV-2026-001",
                "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                LocalDate.now(),
                InvoiceType.SATIS,
                InvoiceProfile.TICARI_FATURA,
                List.of(new CreateInvoiceLineItemRequest(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("10.0000"),
                        new BigDecimal("10000.0000"),
                        new BigDecimal("20.00")
                ))
        );

        when(matchingService.createAndEvaluateInvoice(any(CreateSupplierInvoiceRequest.class))).thenReturn(sampleInvoiceResponse);

        mockMvc.perform(post(Endpoints.Matching.BASE + Endpoints.Matching.INVOICES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-2026-001"))
                .andExpect(jsonPath("$.matchStatus").value("AUTO_MATCHED"));
    }

    @Test
    @DisplayName("GET /api/v1/matching/invoices/{id} - returns invoice by ID")
    void shouldGetInvoiceById() throws Exception {
        when(matchingService.getInvoiceById(invoiceId)).thenReturn(sampleInvoiceResponse);

        mockMvc.perform(get(Endpoints.Matching.BASE + Endpoints.Matching.INVOICES + "/" + invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-2026-001"));
    }

    @Test
    @DisplayName("GET /api/v1/matching/invoices/by-po/{poId} - returns invoices for specific PO")
    void shouldGetInvoicesByPurchaseOrder() throws Exception {
        when(matchingService.getInvoicesByPurchaseOrder(poId)).thenReturn(List.of(sampleInvoiceResponse));

        mockMvc.perform(get(Endpoints.Matching.BASE + Endpoints.Matching.INVOICES + "/by-po/" + poId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceNumber").value("INV-2026-001"))
                .andExpect(jsonPath("$[0].poNumber").value("PO-2026-00001"));
    }

    @Test
    @DisplayName("GET /api/v1/matching/invoices - returns all supplier invoices")
    void shouldGetAllInvoices() throws Exception {
        when(matchingService.getAllInvoices()).thenReturn(List.of(sampleInvoiceResponse));

        mockMvc.perform(get(Endpoints.Matching.BASE + Endpoints.Matching.INVOICES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(invoiceId.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/matching/invoices/{id}/override - manager overrides held invoice")
    void shouldManagerOverrideInvoice() throws Exception {
        ManagerOverrideRequest overrideRequest = new ManagerOverrideRequest("Approved by finance director");

        SupplierInvoiceResponse overriddenResponse = new SupplierInvoiceResponse(
                invoiceId, "INV-2026-001", "ettn-01", LocalDate.now(), InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA,
                poId, "PO-2026-00001", UUID.randomUUID(), "Global Server Supplies", "123", UUID.randomUUID(), "SpendSync Turkey",
                UUID.randomUUID(), "Engineering", "TRY", new BigDecimal("100000.00"), new BigDecimal("20000.00"),
                new BigDecimal("120000.00"), InvoiceMatchStatus.MANUALLY_MATCHED, InvoiceStatus.APPROVED_FOR_PAYMENT,
                null, "Approved by finance director", UUID.randomUUID(), List.of(), Instant.now()
        );

        when(matchingService.managerOverride(eq(invoiceId), any(ManagerOverrideRequest.class))).thenReturn(overriddenResponse);

        mockMvc.perform(post(Endpoints.Matching.BASE + Endpoints.Matching.INVOICES + "/" + invoiceId + "/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overrideRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchStatus").value("MANUALLY_MATCHED"))
                .andExpect(jsonPath("$.status").value("APPROVED_FOR_PAYMENT"));
    }

    @Test
    @DisplayName("POST /api/v1/matching/invoices/{id}/reject - commercially rejects invoice")
    void shouldRejectInvoice() throws Exception {
        RejectInvoiceRequest rejectRequest = new RejectInvoiceRequest("Wrong tax rate applied");

        SupplierInvoiceResponse rejectedResponse = new SupplierInvoiceResponse(
                invoiceId, "INV-2026-001", "ettn-01", LocalDate.now(), InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA,
                poId, "PO-2026-00001", UUID.randomUUID(), "Global Server Supplies", "123", UUID.randomUUID(), "SpendSync Turkey",
                UUID.randomUUID(), "Engineering", "TRY", new BigDecimal("100000.00"), new BigDecimal("20000.00"),
                new BigDecimal("120000.00"), InvoiceMatchStatus.REJECTED, InvoiceStatus.CANCELLED,
                "Wrong tax rate applied", null, null, List.of(), Instant.now()
        );

        when(matchingService.rejectInvoice(eq(invoiceId), any(RejectInvoiceRequest.class))).thenReturn(rejectedResponse);

        mockMvc.perform(post(Endpoints.Matching.BASE + Endpoints.Matching.INVOICES + "/" + invoiceId + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchStatus").value("REJECTED"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
