package com.enterprise.spendsync.payment.web;

import com.enterprise.spendsync.matching.internal.domain.InvoiceProfile;
import com.enterprise.spendsync.matching.internal.domain.InvoiceType;
import com.enterprise.spendsync.payment.internal.domain.PaymentBatchStatus;
import com.enterprise.spendsync.payment.internal.domain.PaymentMethod;
import com.enterprise.spendsync.payment.internal.dto.*;
import com.enterprise.spendsync.payment.internal.service.PaymentService;
import com.enterprise.spendsync.payment.internal.web.PaymentController;
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
@DisplayName("PaymentController REST Web API Slice Tests")
class PaymentControllerWebMvcTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private UUID tenantId;
    private UUID batchId;
    private PaymentBatchResponse sampleBatchResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new StringHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper)
                )
                .build();

        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        batchId = UUID.randomUUID();

        sampleBatchResponse = new PaymentBatchResponse(
                batchId,
                "PAY-2026-00001",
                UUID.randomUUID(),
                "SpendSync Turkey",
                PaymentMethod.ISO_20022_PAIN_001,
                new BigDecimal("120000.00"),
                "TRY",
                1,
                PaymentBatchStatus.DRAFT,
                UUID.randomUUID(),
                "AP Specialist",
                null,
                null,
                null,
                "<pain001/>",
                "idemp-001",
                List.of(),
                Instant.now()
        );
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("GET /api/v1/payments/invoices/due - returns invoices approved for payment")
    void shouldGetDueInvoices() throws Exception {
        DueInvoiceResponse dueInvoice = new DueInvoiceResponse(
                UUID.randomUUID(),
                "INV-2026-001",
                "ettn-001",
                LocalDate.now(),
                UUID.randomUUID(),
                "Global Server Supplies",
                "TR330006200000012345678901",
                UUID.randomUUID(),
                "SpendSync Turkey",
                "TRY",
                new BigDecimal("120000.00"),
                "AUTO_MATCHED",
                "APPROVED_FOR_PAYMENT"
        );

        when(paymentService.getDueInvoices()).thenReturn(List.of(dueInvoice));

        mockMvc.perform(get(Endpoints.Payment.BASE + Endpoints.Payment.DUE_INVOICES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceNumber").value("INV-2026-001"))
                .andExpect(jsonPath("$[0].vendorName").value("Global Server Supplies"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/batches - creates payment batch returning 201 Created")
    void shouldCreatePaymentBatch() throws Exception {
        CreatePaymentBatchRequest request = new CreatePaymentBatchRequest(
                UUID.randomUUID(),
                PaymentMethod.ISO_20022_PAIN_001,
                "idemp-001",
                List.of(UUID.randomUUID())
        );

        when(paymentService.createPaymentBatch(any(CreatePaymentBatchRequest.class))).thenReturn(sampleBatchResponse);

        mockMvc.perform(post(Endpoints.Payment.BASE + Endpoints.Payment.BATCHES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(batchId.toString()))
                .andExpect(jsonPath("$.batchNumber").value("PAY-2026-00001"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/batches/{id} - returns payment batch by ID")
    void shouldGetPaymentBatchById() throws Exception {
        when(paymentService.getPaymentBatchById(batchId)).thenReturn(sampleBatchResponse);

        mockMvc.perform(get(Endpoints.Payment.BASE + Endpoints.Payment.BATCHES + "/" + batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(batchId.toString()))
                .andExpect(jsonPath("$.batchNumber").value("PAY-2026-00001"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/batches - returns all payment batches")
    void shouldGetAllPaymentBatches() throws Exception {
        when(paymentService.getAllPaymentBatches()).thenReturn(List.of(sampleBatchResponse));

        mockMvc.perform(get(Endpoints.Payment.BASE + Endpoints.Payment.BATCHES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].batchNumber").value("PAY-2026-00001"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/batches/{id}/approve - CFO approves and dispatches payment batch")
    void shouldApprovePaymentBatch() throws Exception {
        ApprovePaymentBatchRequest approveReq = new ApprovePaymentBatchRequest("Approved for SEPA / EFT");

        PaymentBatchResponse dispatchedResponse = new PaymentBatchResponse(
                batchId, "PAY-2026-00001", UUID.randomUUID(), "SpendSync Turkey", PaymentMethod.ISO_20022_PAIN_001,
                new BigDecimal("120000.00"), "TRY", 1, PaymentBatchStatus.DISPATCHED, UUID.randomUUID(), "AP Specialist",
                UUID.randomUUID(), "CFO", Instant.now(), "<pain001/>", "idemp-001", List.of(), Instant.now()
        );

        when(paymentService.approveAndDispatchPaymentBatch(eq(batchId), any(ApprovePaymentBatchRequest.class))).thenReturn(dispatchedResponse);

        mockMvc.perform(post(Endpoints.Payment.BASE + Endpoints.Payment.BATCHES + "/" + batchId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISPATCHED"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/batches/{id}/cancel - cancels payment batch")
    void shouldCancelPaymentBatch() throws Exception {
        PaymentBatchResponse cancelledResponse = new PaymentBatchResponse(
                batchId, "PAY-2026-00001", UUID.randomUUID(), "SpendSync Turkey", PaymentMethod.ISO_20022_PAIN_001,
                new BigDecimal("120000.00"), "TRY", 1, PaymentBatchStatus.CANCELLED, UUID.randomUUID(), "AP Specialist",
                null, null, null, "<pain001/>", "idemp-001", List.of(), Instant.now()
        );

        when(paymentService.cancelPaymentBatch(batchId)).thenReturn(cancelledResponse);

        mockMvc.perform(post(Endpoints.Payment.BASE + Endpoints.Payment.BATCHES + "/" + batchId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
