package com.enterprise.spendsync.vendorportal.service;

import com.enterprise.spendsync.audit.internal.service.AuditService;
import com.enterprise.spendsync.core.internal.domain.*;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.matching.internal.domain.*;
import com.enterprise.spendsync.matching.internal.repository.InvoiceDiscrepancyRepository;
import com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceLineItemRepository;
import com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceRepository;
import com.enterprise.spendsync.matching.internal.service.GibXsltRendererService;
import com.enterprise.spendsync.matching.internal.service.UblTrInvoiceParserService;
import com.enterprise.spendsync.purchasing.internal.domain.*;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderRepository;
import com.enterprise.spendsync.purchasing.internal.repository.VendorRepository;
import com.enterprise.spendsync.shared.security.JwtTokenProvider;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import com.enterprise.spendsync.vendorportal.dto.*;
import com.enterprise.spendsync.vendorportal.internal.domain.*;
import com.enterprise.spendsync.vendorportal.internal.repository.*;
import com.enterprise.spendsync.vendorportal.internal.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Vendor Portal Service Integration & Mock Tests (Onboarding, PO-Flip, Tevkifat, Discounting & Form BS)")
class VendorPortalServiceTest {

    // Onboarding Mocks
    @Mock
    private VendorInvitationRepository invitationRepository;
    @Mock
    private VendorUserRepository vendorUserRepository;
    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private AuditService auditService;

    // Bank Governance Mocks
    @Mock
    private VendorBankChangeRequestRepository bankChangeRequestRepository;

    // Invoice & PO-Flip Mocks
    @Mock
    private SupplierInvoiceRepository supplierInvoiceRepository;
    @Mock
    private SupplierInvoiceLineItemRepository invoiceLineItemRepository;
    @Mock
    private InvoiceDiscrepancyRepository discrepancyRepository;
    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private UblTrInvoiceParserService ublParserService;
    @Mock
    private GibXsltRendererService xsltRendererService;

    // Finance & Discounting Mocks
    @Mock
    private VendorEarlyPayOfferRepository earlyPayOfferRepository;
    @Mock
    private VendorCatalogProposalRepository catalogProposalRepository;
    @Mock
    private VendorMonthlyReconciliationRepository reconciliationRepository;

    private VendorOnboardingServiceImpl onboardingService;
    private VendorBankGovernanceServiceImpl bankGovernanceService;
    private VendorInvoiceServiceImpl invoiceService;
    private VendorFinanceServiceImpl financeService;

    private UUID tenantId;
    private Tenant tenant;
    private Vendor vendor;
    private VendorUser vendorUser;
    private PurchaseOrder po;
    private PurchaseOrderLineItem poLine;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("SpendSync Global");

        vendor = new Vendor(
                tenant, "Global IT Hardware Inc.", "9998887776", "Maslak",
                VendorCategory.IT_HARDWARE, VendorTier.TIER_1_STRATEGIC, true,
                "finance@globalit.com", "+90 212 999 0000", "Maslak", "Istanbul", "TR",
                PaymentTerms.NET_30, "Garanti BBVA", "TR330006200000012345678901"
        );
        vendor.setId(UUID.randomUUID());

        vendorUser = new VendorUser(
                tenant, vendor, "vendoradmin@globalit.com", "$2a$10$hashedpass",
                "Ali Yilmaz", "+90 555 111 2233", RoleType.VENDOR_ADMIN, true
        );
        vendorUser.setId(UUID.randomUUID());

        LegalEntity legalEntity = new LegalEntity(tenant, "SpendSync Turkey", "TR01", "1234567890", "TRY", "Istanbul", "TR");
        legalEntity.setId(UUID.randomUUID());

        CostCenter costCenter = new CostCenter(tenant, legalEntity, "CC-IT", "IT Department");
        costCenter.setId(UUID.randomUUID());

        User creator = new User("procurement@spendsync.com", "pass", "Proc", "Officer", null, "TR");
        creator.setId(UUID.randomUUID());

        po = new PurchaseOrder(
                tenant, "PO-2026-00001", null, legalEntity, costCenter, null,
                vendor, Incoterms.DAP, "TRY", PaymentTerms.NET_30, null, creator
        );
        po.setId(UUID.randomUUID());
        po.setStatus(PurchaseOrderStatus.ISSUED);

        poLine = new PurchaseOrderLineItem(
                tenant, po, null, 1, "Server Rack 42U", "IT_HARDWARE",
                new BigDecimal("10.0000"), "PIECE", new BigDecimal("10000.0000"),
                BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now().plusDays(10)
        );
        poLine.setId(UUID.randomUUID());
        po.addLineItem(poLine);

        onboardingService = new VendorOnboardingServiceImpl(
                invitationRepository, vendorUserRepository, vendorRepository, tenantRepository,
                passwordEncoder, jwtTokenProvider, auditService
        );

        bankGovernanceService = new VendorBankGovernanceServiceImpl(
                bankChangeRequestRepository, vendorUserRepository, vendorRepository, auditService
        );

        invoiceService = new VendorInvoiceServiceImpl(
                supplierInvoiceRepository, invoiceLineItemRepository, discrepancyRepository,
                purchaseOrderRepository, vendorUserRepository, ublParserService, xsltRendererService, auditService
        );

        financeService = new VendorFinanceServiceImpl(
                supplierInvoiceRepository, earlyPayOfferRepository, catalogProposalRepository,
                reconciliationRepository, vendorUserRepository, null, auditService
        );
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("TC-09-01: Valid invitation token accepts onboarding, registers VendorUser and issues JWT")
    void shouldAcceptVendorInvitationSuccessfully() {
        VendorInvitation invitation = new VendorInvitation(
                tenant, "finance@globalit.com", "9998887776", "Global IT Hardware Inc.",
                "v_inv_valid123456", Instant.now().plus(5, ChronoUnit.DAYS), UUID.randomUUID()
        );
        invitation.setId(UUID.randomUUID());

        VendorAcceptInviteRequest request = new VendorAcceptInviteRequest(
                "v_inv_valid123456", "Ali Yilmaz", "SecurePass123!", "+90 555 111 2233",
                "Maslak VD", "Maslak Plaza No:5", "Istanbul", "TR",
                "Garanti BBVA", "TR330006200000012345678901"
        );

        when(invitationRepository.findByInvitationToken("v_inv_valid123456")).thenReturn(Optional.of(invitation));
        when(vendorRepository.findByTaxNumberAndTenantId("9998887776", tenantId)).thenReturn(Optional.of(vendor));
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("$2a$10$encodedHash");
        when(vendorUserRepository.save(any(VendorUser.class))).thenAnswer(i -> {
            VendorUser u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtTokenProvider.generateVendorAccessToken(any(VendorUser.class))).thenReturn("vendor-jwt-token-xyz");

        VendorAuthResponse response = onboardingService.acceptInvitation(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("vendor-jwt-token-xyz");
        assertThat(invitation.getStatus()).isEqualTo(VendorInvitationStatus.ACCEPTED);
    }

    @Test
    @DisplayName("TC-09-02: Expired invitation token throws error")
    void shouldRejectExpiredInvitation() {
        VendorInvitation expiredInvitation = new VendorInvitation(
                tenant, "finance@globalit.com", "9998887776", "Global IT Hardware Inc.",
                "v_inv_expired", Instant.now().minus(2, ChronoUnit.DAYS), UUID.randomUUID()
        );

        when(invitationRepository.findByInvitationToken("v_inv_expired")).thenReturn(Optional.of(expiredInvitation));

        VendorAcceptInviteRequest request = new VendorAcceptInviteRequest(
                "v_inv_expired", "Pass123!", "Ali", "+90555", "VD", "Addr", "City", "TR", "Bank", "TR01"
        );

        assertThatThrownBy(() -> onboardingService.acceptInvitation(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invitation is no longer valid");
    }

    @Test
    @DisplayName("TC-09-03: Submitting bank change creates PENDING_REVIEW request and CFO approval updates Vendor IBAN")
    void shouldSubmitAndApproveBankChangeRequest() {
        BankChangeRequestDto.Submission submission = new BankChangeRequestDto.Submission(
                "Akbank T.A.S.", "TR550004600000098765432109", "https://docs.spendsync.com/bank-letter.pdf"
        );

        when(vendorUserRepository.findById(vendorUser.getId())).thenReturn(Optional.of(vendorUser));
        when(bankChangeRequestRepository.save(any(VendorBankChangeRequest.class))).thenAnswer(i -> {
            VendorBankChangeRequest req = i.getArgument(0);
            req.setId(UUID.randomUUID());
            return req;
        });

        BankChangeRequestDto.Response subResponse = bankGovernanceService.submitBankChangeRequest(submission, vendorUser.getId());
        assertThat(subResponse.status()).isEqualTo("PENDING_REVIEW");

        // Now CFO Approves
        VendorBankChangeRequest changeRequest = new VendorBankChangeRequest(
                tenant, vendor, vendorUser, "Akbank T.A.S.", "TR550004600000098765432109", "https://doc"
        );
        changeRequest.setId(UUID.randomUUID());

        when(bankChangeRequestRepository.findById(changeRequest.getId())).thenReturn(Optional.of(changeRequest));

        BankChangeDecisionRequest decision = new BankChangeDecisionRequest("IBAN verified against official bank letter");
        UUID cfoId = UUID.randomUUID();

        BankChangeRequestDto.Response approvedResponse = bankGovernanceService.approveBankChangeRequest(changeRequest.getId(), decision, cfoId);

        assertThat(approvedResponse.status()).isEqualTo("APPROVED");
        assertThat(vendor.getIban()).isEqualTo("TR550004600000098765432109");
        assertThat(vendor.getBankName()).isEqualTo("Akbank T.A.S.");
    }

    @Test
    @DisplayName("TC-09-04: PO-Flip converts PO to e-Invoice calculating 2/10 Tevkifat withholding and net payable")
    void shouldCreatePoFlipInvoiceWithTevkifat() {
        PoFlipInvoiceRequest request = new PoFlipInvoiceRequest(
                "GIB2026000000001",
                "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                InvoiceProfile.TICARI_FATURA,
                InvoiceType.SATIS,
                LocalDate.now(),
                List.of(new PoFlipInvoiceRequest.PoFlipLineItemDto(
                        poLine.getId(),
                        new BigDecimal("10.0000"),
                        new BigDecimal("20.00"),
                        "601",
                        "2/10" // 2/10 Tevkifat on 20,000 TL KDV = 4,000 TL withholding -> 116,000 TL Net Payable
                ))
        );

        when(vendorUserRepository.findById(vendorUser.getId())).thenReturn(Optional.of(vendorUser));
        when(purchaseOrderRepository.findByIdAndTenantIdAndVendorId(po.getId(), tenantId, vendor.getId())).thenReturn(Optional.of(po));
        when(supplierInvoiceRepository.existsByTenantIdAndEttn(tenantId, request.ettn())).thenReturn(false);
        when(supplierInvoiceRepository.existsByTenantIdAndVendorIdAndInvoiceNumber(tenantId, vendor.getId(), request.invoiceNumber())).thenReturn(false);
        when(invoiceLineItemRepository.findAllByTenantIdAndPurchaseOrderLineItemId(tenantId, poLine.getId())).thenReturn(List.of());

        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenAnswer(i -> {
            SupplierInvoice inv = i.getArgument(0);
            inv.setId(UUID.randomUUID());
            return inv;
        });

        SupplierInvoiceResponse response = invoiceService.createPoFlipInvoice(po.getId(), request, vendorUser.getId());

        assertThat(response).isNotNull();
        assertThat(response.invoiceNumber()).isEqualTo("GIB2026000000001");
        assertThat(response.subtotalAmount()).isEqualByComparingTo("100000.0000");
        assertThat(response.taxAmount()).isEqualByComparingTo("20000.0000");
        assertThat(response.withholdingTaxAmount()).isEqualByComparingTo("4000.0000");
        assertThat(response.totalAmount()).isEqualByComparingTo("120000.0000");
        assertThat(response.payableAmount()).isEqualByComparingTo("116000.0000");
    }

    @Test
    @DisplayName("TC-09-10: Reject PO-Flip invoice with duplicate ETTN")
    void shouldRejectDuplicateEttnPoFlip() {
        PoFlipInvoiceRequest request = new PoFlipInvoiceRequest(
                "GIB2026000000002",
                "duplicate-ettn-uuid",
                InvoiceProfile.TICARI_FATURA,
                InvoiceType.SATIS,
                LocalDate.now(),
                List.of(new PoFlipInvoiceRequest.PoFlipLineItemDto(
                        poLine.getId(),
                        new BigDecimal("1.0000"),
                        new BigDecimal("20.00"),
                        "601",
                        "2/10"
                ))
        );

        when(vendorUserRepository.findById(vendorUser.getId())).thenReturn(Optional.of(vendorUser));
        when(purchaseOrderRepository.findByIdAndTenantIdAndVendorId(po.getId(), tenantId, vendor.getId())).thenReturn(Optional.of(po));
        when(supplierInvoiceRepository.existsByTenantIdAndEttn(tenantId, "duplicate-ettn-uuid")).thenReturn(true);

        assertThatThrownBy(() -> invoiceService.createPoFlipInvoice(po.getId(), request, vendorUser.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("TC-09-13: Cannot accept already processed or invalid early payment offer")
    void shouldRejectAcceptingNonExistentOrAlreadyProcessedOffer() {
        SupplierInvoice invoice = new SupplierInvoice(
                tenant, "INV-2026-001", "ettn-001", LocalDate.now(), InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA,
                po, vendor, po.getLegalEntity(), po.getCostCenter(), "TRY",
                new BigDecimal("100000.00"), new BigDecimal("20000.00"), new BigDecimal("120000.00")
        );
        invoice.setId(UUID.randomUUID());

        when(vendorUserRepository.findById(vendorUser.getId())).thenReturn(Optional.of(vendorUser));
        when(supplierInvoiceRepository.findByIdAndTenantIdAndVendorId(invoice.getId(), tenantId, vendor.getId()))
                .thenReturn(Optional.of(invoice));
        when(earlyPayOfferRepository.findByTenantIdAndSupplierInvoiceIdAndStatus(tenantId, invoice.getId(), EarlyPayOfferStatus.OFFERED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> financeService.acceptEarlyPaymentOffer(invoice.getId(), vendorUser.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No active early payment offer found");
    }

    @Test
    @DisplayName("TC-09-12 & TC-09-13: Early payment discount generation and acceptance accelerates payout to T+3")
    void shouldGenerateAndAcceptEarlyPaymentDiscount() {
        SupplierInvoice approvedInvoice = new SupplierInvoice(
                tenant, "INV-2026-001", "ettn-001", LocalDate.now(), InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA,
                po, vendor, po.getLegalEntity(), po.getCostCenter(), "TRY",
                new BigDecimal("100000.00"), new BigDecimal("20000.00"), new BigDecimal("120000.00")
        );
        approvedInvoice.setId(UUID.randomUUID());
        approvedInvoice.setStatus(InvoiceStatus.APPROVED_FOR_PAYMENT);

        when(vendorUserRepository.findById(vendorUser.getId())).thenReturn(Optional.of(vendorUser));
        when(supplierInvoiceRepository.findAllByTenantIdAndVendorIdAndStatusOrderByCreatedAtDesc(tenantId, vendor.getId(), InvoiceStatus.APPROVED_FOR_PAYMENT))
                .thenReturn(List.of(approvedInvoice));
        when(earlyPayOfferRepository.findByTenantIdAndSupplierInvoiceId(tenantId, approvedInvoice.getId()))
                .thenReturn(Optional.empty());

        when(earlyPayOfferRepository.save(any(VendorEarlyPayOffer.class))).thenAnswer(i -> {
            VendorEarlyPayOffer o = i.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        List<EarlyPayOfferResponse> offers = financeService.getAvailableEarlyPaymentOffers(vendorUser.getId());

        assertThat(offers).hasSize(1);
        assertThat(offers.get(0).discountPercentage()).isEqualByComparingTo("2.00");
        assertThat(offers.get(0).discountAmount()).isEqualByComparingTo("2400.0000"); // 2% of 120,000
        assertThat(offers.get(0).netPayoutAmount()).isEqualByComparingTo("117600.0000");

        // Accept Offer
        VendorEarlyPayOffer activeOffer = new VendorEarlyPayOffer(
                tenant, approvedInvoice, vendor, new BigDecimal("120000.00"), LocalDate.now().plusDays(30),
                new BigDecimal("2.00"), new BigDecimal("2400.00"), new BigDecimal("117600.00"), LocalDate.now().plusDays(3)
        );
        activeOffer.setId(UUID.randomUUID());

        when(supplierInvoiceRepository.findByIdAndTenantIdAndVendorId(approvedInvoice.getId(), tenantId, vendor.getId()))
                .thenReturn(Optional.of(approvedInvoice));
        when(earlyPayOfferRepository.findByTenantIdAndSupplierInvoiceIdAndStatus(tenantId, approvedInvoice.getId(), EarlyPayOfferStatus.OFFERED))
                .thenReturn(Optional.of(activeOffer));

        AcceptEarlyDiscountResponse acceptResponse = financeService.acceptEarlyPaymentOffer(approvedInvoice.getId(), vendorUser.getId());

        assertThat(acceptResponse.status()).isEqualTo("ACCEPTED");
        assertThat(acceptResponse.netPayoutAmount()).isEqualByComparingTo("117600.00");
        assertThat(activeOffer.getStatus()).isEqualTo(EarlyPayOfferStatus.ACCEPTED);
        assertThat(approvedInvoice.getPayableAmount()).isEqualByComparingTo("117600.00");
    }

    @Test
    @DisplayName("TC-09-14 & TC-09-15: Form BS Monthly e-Reconciliation computes invoice aggregates and seals with SHA-256")
    void shouldGenerateAndApproveMonthlyReconciliationWithSha256Seal() {
        SupplierInvoice invoice = new SupplierInvoice(
                tenant, "INV-2026-001", "ettn-001", LocalDate.now(), InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA,
                po, vendor, po.getLegalEntity(), po.getCostCenter(), "TRY",
                new BigDecimal("100000.00"), new BigDecimal("20000.00"), new BigDecimal("120000.00")
        );
        invoice.setId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.APPROVED_FOR_PAYMENT);

        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

        when(vendorUserRepository.findById(vendorUser.getId())).thenReturn(Optional.of(vendorUser));
        when(reconciliationRepository.findByTenantIdAndVendorIdAndPeriodYearAndPeriodMonth(tenantId, vendor.getId(), currentYear, currentMonth))
                .thenReturn(Optional.empty());
        when(supplierInvoiceRepository.findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(tenantId, vendor.getId()))
                .thenReturn(List.of(invoice));

        when(reconciliationRepository.save(any(VendorMonthlyReconciliation.class))).thenAnswer(i -> {
            VendorMonthlyReconciliation r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        MonthlyReconciliationResponse recResponse = financeService.getMonthlyReconciliation(currentYear, currentMonth, vendorUser.getId());

        assertThat(recResponse.invoiceCount()).isEqualTo(1);
        assertThat(recResponse.totalAmount()).isEqualByComparingTo("120000.00");
        assertThat(recResponse.status()).isEqualTo("PENDING");

        // Approve Reconciliation
        MonthlyReconciliationApprovalRequest approvalRequest = new MonthlyReconciliationApprovalRequest(
                currentYear, currentMonth, "All 1 invoice verified against general ledger", false
        );

        VendorMonthlyReconciliation pendingRec = new VendorMonthlyReconciliation(
                tenant, vendor, currentYear, currentMonth, 1, new BigDecimal("120000.00")
        );
        pendingRec.setId(UUID.randomUUID());

        when(reconciliationRepository.findByTenantIdAndVendorIdAndPeriodYearAndPeriodMonth(tenantId, vendor.getId(), currentYear, currentMonth))
                .thenReturn(Optional.of(pendingRec));

        MonthlyReconciliationResponse approvedRecResponse = financeService.approveMonthlyReconciliation(approvalRequest, vendorUser.getId());

        assertThat(approvedRecResponse.status()).isEqualTo("APPROVED");
        assertThat(approvedRecResponse.signedChecksum()).isNotNull().hasSize(64);
        assertThat(pendingRec.getSignedChecksum()).isNotNull().hasSize(64);
    }
}
