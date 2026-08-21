package com.enterprise.spendsync.purchasing.web;

import com.enterprise.spendsync.purchasing.internal.domain.Incoterms;
import com.enterprise.spendsync.purchasing.internal.domain.PaymentTerms;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderStatus;
import com.enterprise.spendsync.purchasing.internal.dto.*;
import com.enterprise.spendsync.purchasing.internal.service.PurchaseOrderService;
import com.enterprise.spendsync.purchasing.internal.web.PurchaseOrderController;
import com.enterprise.spendsync.shared.config.Endpoints;
import com.enterprise.spendsync.shared.domain.CrossAssignmentWarning;
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
@DisplayName("PurchaseOrderController REST Web API Slice Tests")
class PurchaseOrderControllerWebMvcTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private PurchaseOrderService purchaseOrderService;

    @InjectMocks
    private PurchaseOrderController purchaseOrderController;

    private UUID tenantId;
    private UUID poId;
    private PurchaseOrderDetailResponse samplePoDetail;
    private PurchaseOrderSummaryResponse samplePoSummary;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(purchaseOrderController)
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new StringHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper)
                )
                .build();

        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        poId = UUID.randomUUID();
        samplePoDetail = new PurchaseOrderDetailResponse(
                poId,
                "PO-2026-00001",
                0,
                UUID.randomUUID(),
                "PR-2026-00001",
                UUID.randomUUID(),
                "SpendSync TR A.S.",
                UUID.randomUUID(),
                "Engineering",
                UUID.randomUUID(),
                "HQ Warehouse",
                UUID.randomUUID(),
                "Global Tech Inc.",
                "1234567890",
                "orders@globaltech.com",
                PurchaseOrderStatus.DRAFT,
                Incoterms.DAP,
                "TRY",
                new BigDecimal("50000.00"),
                PaymentTerms.NET_30,
                "Urgent delivery",
                null,
                UUID.randomUUID(),
                "Buyer Officer",
                List.of(),
                List.of(),
                CrossAssignmentWarning.none(),
                Instant.now(),
                Instant.now()
        );

        samplePoSummary = new PurchaseOrderSummaryResponse(
                poId,
                "PO-2026-00001",
                0,
                "PR-2026-00001",
                "SpendSync TR A.S.",
                "Engineering",
                "HQ Warehouse",
                "Global Tech Inc.",
                PurchaseOrderStatus.DRAFT,
                Incoterms.DAP,
                "TRY",
                new BigDecimal("50000.00"),
                1,
                false,
                null,
                Instant.now()
        );
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("POST /api/v1/purchasing/orders - creates Purchase Order returning 201 Created")
    void shouldCreatePurchaseOrder() throws Exception {
        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Incoterms.DAP,
                PaymentTerms.NET_30,
                "TRY",
                "Deliver by month end",
                List.of(new POLineItemRequest(
                        null, "Server Rack", "Hardware", new BigDecimal("2"), "PIECE",
                        new BigDecimal("25000.00"), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now().plusDays(10)
                ))
        );

        when(purchaseOrderService.createPurchaseOrder(any(CreatePurchaseOrderRequest.class))).thenReturn(samplePoDetail);

        mockMvc.perform(post(Endpoints.Purchasing.ORDERS_BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.poNumber").value("PO-2026-00001"))
                .andExpect(jsonPath("$.totalAmount").value(50000.00));
    }

    @Test
    @DisplayName("GET /api/v1/purchasing/orders/{id} - returns single PO details")
    void shouldGetPurchaseOrderById() throws Exception {
        when(purchaseOrderService.getPurchaseOrderById(poId)).thenReturn(samplePoDetail);

        mockMvc.perform(get(Endpoints.Purchasing.ORDERS_BASE + "/" + poId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(poId.toString()))
                .andExpect(jsonPath("$.poNumber").value("PO-2026-00001"));
    }

    @Test
    @DisplayName("GET /api/v1/purchasing/orders - returns list of PO summaries")
    void shouldGetAllPurchaseOrders() throws Exception {
        when(purchaseOrderService.getAllPurchaseOrders(eq(PurchaseOrderStatus.DRAFT), any()))
                .thenReturn(List.of(samplePoSummary));

        mockMvc.perform(get(Endpoints.Purchasing.ORDERS_BASE)
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].poNumber").value("PO-2026-00001"))
                .andExpect(jsonPath("$[0].status").value("DRAFT"));
    }

    @Test
    @DisplayName("POST /api/v1/purchasing/orders/{id}/issue - issues PO to vendor")
    void shouldIssuePurchaseOrder() throws Exception {
        PurchaseOrderDetailResponse issuedResponse = new PurchaseOrderDetailResponse(
                poId, "PO-2026-00001", 0, null, null, UUID.randomUUID(), "SpendSync TR",
                UUID.randomUUID(), "Eng", UUID.randomUUID(), "Dock", UUID.randomUUID(), "Global Tech",
                "123", "orders@gt.com", PurchaseOrderStatus.ISSUED, Incoterms.DAP, "TRY",
                new BigDecimal("50000.00"), PaymentTerms.NET_30, null, Instant.now(),
                UUID.randomUUID(), "Buyer", List.of(), List.of(), CrossAssignmentWarning.none(),
                Instant.now(), Instant.now()
        );

        when(purchaseOrderService.issuePurchaseOrder(poId)).thenReturn(issuedResponse);

        mockMvc.perform(post(Endpoints.Purchasing.ORDERS_BASE + "/" + poId + "/issue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"));
    }

    @Test
    @DisplayName("POST /api/v1/purchasing/orders/{id}/revise - revises PO with scope/price change")
    void shouldRevisePurchaseOrder() throws Exception {
        RevisePurchaseOrderRequest reviseRequest = new RevisePurchaseOrderRequest(
                "Added 1 more unit",
                List.of(new RevisePOLineItemRequest(UUID.randomUUID(), "Server Rack", "Hardware", new BigDecimal("3"), "PIECE", new BigDecimal("25000.00"), null, null, null))
        );

        PurchaseOrderDetailResponse revisedResponse = new PurchaseOrderDetailResponse(
                poId, "PO-2026-00001", 1, null, null, UUID.randomUUID(), "SpendSync TR",
                UUID.randomUUID(), "Eng", UUID.randomUUID(), "Dock", UUID.randomUUID(), "Global Tech",
                "123", "orders@gt.com", PurchaseOrderStatus.ISSUED, Incoterms.DAP, "TRY",
                new BigDecimal("75000.00"), PaymentTerms.NET_30, null, Instant.now(),
                UUID.randomUUID(), "Buyer", List.of(), List.of(), CrossAssignmentWarning.none(),
                Instant.now(), Instant.now()
        );

        when(purchaseOrderService.revisePurchaseOrder(eq(poId), any(RevisePurchaseOrderRequest.class)))
                .thenReturn(revisedResponse);

        mockMvc.perform(post(Endpoints.Purchasing.ORDERS_BASE + "/" + poId + "/revise")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviseRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisionNumber").value(1))
                .andExpect(jsonPath("$.totalAmount").value(75000.00));
    }

    @Test
    @DisplayName("POST /api/v1/purchasing/orders/{id}/cancel - cancels PO")
    void shouldCancelPurchaseOrder() throws Exception {
        CancelPurchaseOrderRequest cancelRequest = new CancelPurchaseOrderRequest("Duplicate PO created");

        PurchaseOrderDetailResponse cancelledResponse = new PurchaseOrderDetailResponse(
                poId, "PO-2026-00001", 0, null, null, UUID.randomUUID(), "SpendSync TR",
                UUID.randomUUID(), "Eng", UUID.randomUUID(), "Dock", UUID.randomUUID(), "Global Tech",
                "123", "orders@gt.com", PurchaseOrderStatus.CANCELLED, Incoterms.DAP, "TRY",
                new BigDecimal("50000.00"), PaymentTerms.NET_30, null, null,
                UUID.randomUUID(), "Buyer", List.of(), List.of(), CrossAssignmentWarning.none(),
                Instant.now(), Instant.now()
        );

        when(purchaseOrderService.cancelPurchaseOrder(eq(poId), any(CancelPurchaseOrderRequest.class)))
                .thenReturn(cancelledResponse);

        mockMvc.perform(post(Endpoints.Purchasing.ORDERS_BASE + "/" + poId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
