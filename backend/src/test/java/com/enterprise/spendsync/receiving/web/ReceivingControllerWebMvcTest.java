package com.enterprise.spendsync.receiving.web;

import com.enterprise.spendsync.purchasing.internal.domain.Incoterms;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderStatus;
import com.enterprise.spendsync.receiving.internal.domain.GoodsReceiptStatus;
import com.enterprise.spendsync.receiving.internal.dto.*;
import com.enterprise.spendsync.receiving.internal.service.GoodsReceiptService;
import com.enterprise.spendsync.receiving.internal.web.GoodsReceiptController;
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
@DisplayName("GoodsReceiptController REST Web API Slice Tests")
class ReceivingControllerWebMvcTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private GoodsReceiptService goodsReceiptService;

    @InjectMocks
    private GoodsReceiptController goodsReceiptController;

    private UUID tenantId;
    private UUID grId;
    private UUID poId;
    private GoodsReceiptResponse sampleGrResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(goodsReceiptController)
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new StringHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper)
                )
                .build();

        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        grId = UUID.randomUUID();
        poId = UUID.randomUUID();

        sampleGrResponse = new GoodsReceiptResponse(
                grId,
                "GR-2026-00001",
                poId,
                "PO-2026-00001",
                UUID.randomUUID(),
                "Global Server Supplies",
                UUID.randomUUID(),
                "Main Warehouse",
                "IRS-2026-99900",
                LocalDate.now(),
                UUID.randomUUID(),
                "Warehouse Clerk",
                GoodsReceiptStatus.COMPLETED,
                "All items verified",
                List.of(new GRLineItemResponse(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Server Rack 42U",
                        "IT_HARDWARE",
                        "PIECE",
                        new BigDecimal("10.0000"),
                        new BigDecimal("10.0000"),
                        new BigDecimal("10.0000"),
                        BigDecimal.ZERO,
                        null,
                        "Verified OK",
                        Instant.now()
                )),
                Instant.now()
        );
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("POST /api/v1/receiving/receipts - creates Goods Receipt Note returning 201 Created")
    void shouldCreateGoodsReceipt() throws Exception {
        CreateGoodsReceiptRequest request = new CreateGoodsReceiptRequest(
                poId,
                "IRS-2026-99900",
                LocalDate.now(),
                UUID.randomUUID(),
                "All items verified",
                List.of(new CreateGRLineItemRequest(
                        UUID.randomUUID(),
                        new BigDecimal("10.0000"),
                        new BigDecimal("10.0000"),
                        BigDecimal.ZERO,
                        null,
                        "Verified OK"
                ))
        );

        when(goodsReceiptService.createGoodsReceipt(any(CreateGoodsReceiptRequest.class))).thenReturn(sampleGrResponse);

        mockMvc.perform(post(Endpoints.Receiving.BASE + Endpoints.Receiving.RECEIPTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptNumber").value("GR-2026-00001"))
                .andExpect(jsonPath("$.waybillNumber").value("IRS-2026-99900"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/v1/receiving/receipts - returns all Goods Receipt Notes")
    void shouldGetAllGoodsReceipts() throws Exception {
        when(goodsReceiptService.getAllGoodsReceipts()).thenReturn(List.of(sampleGrResponse));

        mockMvc.perform(get(Endpoints.Receiving.BASE + Endpoints.Receiving.RECEIPTS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(grId.toString()))
                .andExpect(jsonPath("$[0].receiptNumber").value("GR-2026-00001"));
    }

    @Test
    @DisplayName("GET /api/v1/receiving/receipts/{id} - returns Goods Receipt Note by ID")
    void shouldGetGoodsReceiptById() throws Exception {
        when(goodsReceiptService.getGoodsReceiptById(grId)).thenReturn(sampleGrResponse);

        mockMvc.perform(get(Endpoints.Receiving.BASE + Endpoints.Receiving.RECEIPTS + "/" + grId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(grId.toString()))
                .andExpect(jsonPath("$.receiptNumber").value("GR-2026-00001"));
    }

    @Test
    @DisplayName("GET /api/v1/receiving/receipts/by-po/{poId} - returns GRNs for specific PO")
    void shouldGetGoodsReceiptsByPurchaseOrder() throws Exception {
        when(goodsReceiptService.getGoodsReceiptsByPurchaseOrder(poId)).thenReturn(List.of(sampleGrResponse));

        mockMvc.perform(get(Endpoints.Receiving.BASE + Endpoints.Receiving.RECEIPTS + "/by-po/" + poId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].receiptNumber").value("GR-2026-00001"))
                .andExpect(jsonPath("$[0].poNumber").value("PO-2026-00001"));
    }

    @Test
    @DisplayName("GET /api/v1/receiving/orders/pending - returns pending POs ready for dock receiving")
    void shouldGetPendingOrdersForReceiving() throws Exception {
        PendingPOForReceivingResponse pendingResponse = new PendingPOForReceivingResponse(
                poId,
                "PO-2026-00001",
                UUID.randomUUID(),
                "Global Server Supplies",
                UUID.randomUUID(),
                "Main Warehouse",
                PurchaseOrderStatus.ISSUED,
                Incoterms.DAP,
                new BigDecimal("100000.00"),
                "TRY",
                1,
                Instant.now()
        );

        when(goodsReceiptService.getPendingOrdersForReceiving()).thenReturn(List.of(pendingResponse));

        mockMvc.perform(get(Endpoints.Receiving.BASE + Endpoints.Receiving.PENDING_ORDERS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].poNumber").value("PO-2026-00001"))
                .andExpect(jsonPath("$[0].status").value("ISSUED"));
    }
}
