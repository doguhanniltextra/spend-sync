package com.enterprise.spendsync.vendorportal.web;

import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.matching.internal.domain.InvoiceProfile;
import com.enterprise.spendsync.matching.internal.domain.InvoiceType;
import com.enterprise.spendsync.shared.config.Endpoints;
import com.enterprise.spendsync.shared.security.UserPrincipal;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import com.enterprise.spendsync.vendorportal.dto.*;
import com.enterprise.spendsync.vendorportal.internal.service.VendorAuthService;
import com.enterprise.spendsync.vendorportal.internal.service.VendorFinanceService;
import com.enterprise.spendsync.vendorportal.internal.service.VendorInvoiceService;
import com.enterprise.spendsync.vendorportal.internal.service.VendorOnboardingService;
import com.enterprise.spendsync.vendorportal.internal.web.VendorAuthController;
import com.enterprise.spendsync.vendorportal.internal.web.VendorFinanceController;
import com.enterprise.spendsync.vendorportal.internal.web.VendorInvoiceController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("VendorPortal REST Web API Slice Tests (Auth, PO-Flip, Early Pay & Reconciliation)")
class VendorPortalControllerWebMvcTest {

    private MockMvc authMockMvc;
    private MockMvc invoiceMockMvc;
    private MockMvc financeMockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private VendorOnboardingService onboardingService;
    @Mock
    private VendorAuthService authService;
    @Mock
    private VendorInvoiceService invoiceService;
    @Mock
    private VendorFinanceService financeService;

    @InjectMocks
    private VendorAuthController authController;
    @InjectMocks
    private VendorInvoiceController invoiceController;
    @InjectMocks
    private VendorFinanceController financeController;

    private UUID tenantId;
    private UUID vendorId;
    private UUID vendorUserId;
    private UserPrincipal vendorPrincipal;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        vendorUserId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        vendorPrincipal = new UserPrincipal(
                vendorUserId,
                tenantId,
                vendorId,
                "VENDOR",
                "vendoradmin@globalit.com",
                null,
                "Ali Yilmaz",
                true,
                Set.of(RoleType.VENDOR_ADMIN),
                Set.of()
        );

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class) ||
                        parameter.getParameterType().equals(UserPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return vendorPrincipal;
            }
        };

        authMockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setMessageConverters(new ByteArrayHttpMessageConverter(), new StringHttpMessageConverter(), new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        invoiceMockMvc = MockMvcBuilders.standaloneSetup(invoiceController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setMessageConverters(new ByteArrayHttpMessageConverter(), new StringHttpMessageConverter(), new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        financeMockMvc = MockMvcBuilders.standaloneSetup(financeController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setMessageConverters(new ByteArrayHttpMessageConverter(), new StringHttpMessageConverter(), new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("GET /api/v1/vendor-portal/auth/invite/{token} - retrieves onboarding invitation details")
    void shouldGetInvitationDetails() throws Exception {
        VendorInvitationDetailsResponse response = new VendorInvitationDetailsResponse(
                UUID.randomUUID(), "v_token_123", "finance@globalit.com", "9998887776", "Global IT Hardware Inc.", "PENDING", Instant.now().plusSeconds(3600)
        );

        when(onboardingService.getInvitationDetails("v_token_123")).thenReturn(response);

        authMockMvc.perform(get(Endpoints.VendorPortal.AUTH_BASE + Endpoints.VendorPortal.INVITE_DETAILS.replace("{token}", "v_token_123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Global IT Hardware Inc."))
                .andExpect(jsonPath("$.email").value("finance@globalit.com"));
    }

    @Test
    @DisplayName("POST /api/v1/vendor-portal/auth/accept-invite - accepts invitation returning 201 Created")
    void shouldAcceptInvitation() throws Exception {
        VendorAcceptInviteRequest request = new VendorAcceptInviteRequest(
                "v_token_123", "Ali Yilmaz", "SecurePass123!", "+90555", "VD", "Addr", "City", "TR", "Bank", "TR01"
        );

        VendorAuthResponse authResponse = new VendorAuthResponse(
                "jwt-token-123", "Bearer", 86400L, vendorUserId, vendorId, tenantId,
                "finance@globalit.com", "Ali Yilmaz", "Global IT Hardware Inc.", Set.of("ROLE_VENDOR_ADMIN")
        );

        when(onboardingService.acceptInvitation(any(VendorAcceptInviteRequest.class))).thenReturn(authResponse);

        authMockMvc.perform(post(Endpoints.VendorPortal.AUTH_BASE + Endpoints.VendorPortal.ACCEPT_INVITE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("jwt-token-123"))
                .andExpect(jsonPath("$.companyName").value("Global IT Hardware Inc."));
    }

    @Test
    @DisplayName("POST /api/v1/vendor-portal/invoices/po/{poId}/flip - creates PO-Flip invoice returning 201 Created")
    void shouldCreatePoFlipInvoice() throws Exception {
        UUID poId = UUID.randomUUID();
        PoFlipInvoiceRequest request = new PoFlipInvoiceRequest(
                "GIB2026000000001",
                "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                InvoiceProfile.TICARI_FATURA,
                InvoiceType.SATIS,
                LocalDate.now(),
                List.of(new PoFlipInvoiceRequest.PoFlipLineItemDto(
                        UUID.randomUUID(), new BigDecimal("10.00"), new BigDecimal("20.00"), "601", "2/10"
                ))
        );

        SupplierInvoiceResponse response = new SupplierInvoiceResponse(
                UUID.randomUUID(), poId, "PO-2026-00001", "GIB2026000000001", "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "TICARI_FATURA", "SATIS", LocalDate.now(), LocalDate.now().plusDays(30), "TRY",
                new BigDecimal("100000.00"), new BigDecimal("20000.00"), new BigDecimal("4000.00"),
                new BigDecimal("120000.00"), new BigDecimal("116000.00"), "THREE_WAY", "AUTO_MATCHED",
                "SUBMITTED", null, Instant.now()
        );

        when(invoiceService.createPoFlipInvoice(eq(poId), any(PoFlipInvoiceRequest.class), eq(vendorUserId))).thenReturn(response);

        invoiceMockMvc.perform(post(Endpoints.VendorPortal.INVOICES_BASE + Endpoints.VendorPortal.INVOICE_PO_FLIP.replace("{poId}", poId.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").value("GIB2026000000001"))
                .andExpect(jsonPath("$.payableAmount").value(116000.00));
    }

    @Test
    @DisplayName("GET /api/v1/vendor-portal/finance/early-pay-offers - returns dynamic early payment offers")
    void shouldGetEarlyPaymentOffers() throws Exception {
        EarlyPayOfferResponse offer = new EarlyPayOfferResponse(
                UUID.randomUUID(), UUID.randomUUID(), "INV-2026-001", new BigDecimal("120000.00"),
                "TRY", new BigDecimal("2.00"), new BigDecimal("2400.00"), new BigDecimal("117600.00"),
                LocalDate.now().plusDays(30), LocalDate.now().plusDays(3), "OFFERED"
        );

        when(financeService.getAvailableEarlyPaymentOffers(vendorUserId)).thenReturn(List.of(offer));

        financeMockMvc.perform(get(Endpoints.VendorPortal.FINANCE_BASE + Endpoints.VendorPortal.EARLY_PAY_OFFERS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceNumber").value("INV-2026-001"))
                .andExpect(jsonPath("$[0].discountPercentage").value(2.00));
    }

    @Test
    @DisplayName("GET /api/v1/vendor-portal/finance/reconciliation - retrieves Form BS monthly reconciliation")
    void shouldGetMonthlyReconciliation() throws Exception {
        MonthlyReconciliationResponse recResponse = new MonthlyReconciliationResponse(
                UUID.randomUUID(), vendorId, "Global IT Hardware Inc.", 2026, 8, 5,
                new BigDecimal("450000.00"), "TRY", "PENDING", null, null, null
        );

        when(financeService.getMonthlyReconciliation(2026, 8, vendorUserId)).thenReturn(recResponse);

        financeMockMvc.perform(get(Endpoints.VendorPortal.FINANCE_BASE + Endpoints.VendorPortal.RECONCILIATION)
                        .param("year", "2026")
                        .param("month", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(8))
                .andExpect(jsonPath("$.invoiceCount").value(5))
                .andExpect(jsonPath("$.totalAmount").value(450000.00));
    }

    @Test
    @DisplayName("POST /api/v1/vendor-portal/finance/reconciliation/approve - approves reconciliation with SHA-256 seal")
    void shouldApproveMonthlyReconciliation() throws Exception {
        MonthlyReconciliationApprovalRequest request = new MonthlyReconciliationApprovalRequest(
                2026, 8, "All invoices verified", false
        );

        MonthlyReconciliationResponse approvedResponse = new MonthlyReconciliationResponse(
                UUID.randomUUID(), vendorId, "Global IT Hardware Inc.", 2026, 8, 5,
                new BigDecimal("450000.00"), "TRY", "APPROVED", "All invoices verified",
                Instant.now(), "a".repeat(64)
        );

        when(financeService.approveMonthlyReconciliation(any(MonthlyReconciliationApprovalRequest.class), eq(vendorUserId)))
                .thenReturn(approvedResponse);

        financeMockMvc.perform(post(Endpoints.VendorPortal.FINANCE_BASE + Endpoints.VendorPortal.RECONCILIATION_APPROVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.signedChecksum").value("a".repeat(64)));
    }
}
