package com.enterprise.spendsync.vendorportal.service;

import com.enterprise.spendsync.audit.internal.service.AuditService;
import com.enterprise.spendsync.catalog.internal.domain.CatalogCategory;
import com.enterprise.spendsync.catalog.internal.domain.CatalogItem;
import com.enterprise.spendsync.catalog.internal.repository.CatalogItemRepository;
import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.matching.internal.domain.InvoiceMatchStatus;
import com.enterprise.spendsync.matching.internal.domain.InvoiceProfile;
import com.enterprise.spendsync.matching.internal.domain.InvoiceStatus;
import com.enterprise.spendsync.matching.internal.domain.InvoiceType;
import com.enterprise.spendsync.matching.internal.domain.MatchType;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoice;
import com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceRepository;
import com.enterprise.spendsync.purchasing.internal.domain.PaymentTerms;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import com.enterprise.spendsync.purchasing.internal.domain.VendorCategory;
import com.enterprise.spendsync.purchasing.internal.domain.VendorTier;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import com.enterprise.spendsync.vendorportal.dto.AcceptEarlyDiscountResponse;
import com.enterprise.spendsync.vendorportal.dto.EarlyPayOfferResponse;
import com.enterprise.spendsync.vendorportal.dto.InvoicePaymentStatusResponse;
import com.enterprise.spendsync.vendorportal.dto.MonthlyReconciliationApprovalRequest;
import com.enterprise.spendsync.vendorportal.dto.MonthlyReconciliationResponse;
import com.enterprise.spendsync.vendorportal.dto.StatementOfAccountsResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorCatalogProposalRequest;
import com.enterprise.spendsync.vendorportal.dto.VendorCatalogProposalResponse;
import com.enterprise.spendsync.vendorportal.internal.domain.EarlyPayOfferStatus;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorCatalogProposal;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorEarlyPayOffer;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorMonthlyReconciliation;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorUser;
import com.enterprise.spendsync.vendorportal.internal.repository.VendorCatalogProposalRepository;
import com.enterprise.spendsync.vendorportal.internal.repository.VendorEarlyPayOfferRepository;
import com.enterprise.spendsync.vendorportal.internal.repository.VendorMonthlyReconciliationRepository;
import com.enterprise.spendsync.vendorportal.internal.repository.VendorUserRepository;
import com.enterprise.spendsync.vendorportal.internal.service.VendorFinanceServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
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
public class VendorFinanceServiceComprehensiveTest {

    @Mock private SupplierInvoiceRepository supplierInvoiceRepository;
    @Mock private VendorEarlyPayOfferRepository earlyPayOfferRepository;
    @Mock private VendorCatalogProposalRepository catalogProposalRepository;
    @Mock private VendorMonthlyReconciliationRepository reconciliationRepository;
    @Mock private VendorUserRepository vendorUserRepository;
    @Mock private CatalogItemRepository catalogItemRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private VendorFinanceServiceImpl financeService;

    private UUID tenantId;
    private UUID vendorId;
    private UUID vendorUserId;
    private Tenant tenant;
    private Vendor vendor;
    private VendorUser vendorUser;
    private LegalEntity legalEntity;
    private CostCenter costCenter;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        vendorUserId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        tenant = new Tenant("Finance Corp", "finance-corp");
        tenant.setId(tenantId);

        legalEntity = new LegalEntity(tenant, "Finance TR", "LE-TR", "1234567890", "TRY", "Istanbul", "TR");
        legalEntity.setId(UUID.randomUUID());

        costCenter = new CostCenter(tenant, legalEntity, "CC-FIN", "Finance");
        costCenter.setId(UUID.randomUUID());

        vendor = new Vendor(tenant, "Global Supplier", "1234567890", "Istanbul", VendorCategory.IT_HARDWARE,
                VendorTier.TIER_1_STRATEGIC, true, "supplier@global.com", "555-1111",
                "Address", "Istanbul", "TR", PaymentTerms.NET_30, "Garanti", "TR9988776655");
        vendor.setId(vendorId);

        vendorUser = new VendorUser(tenant, vendor, "fin@global.com", "secretHash",
                "Finance Lead", "555-2222", RoleType.VENDOR_ADMIN, true);
        vendorUser.setId(vendorUserId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // =========================================================================
    // 1. getInvoicePaymentStatus
    // =========================================================================

    @Test
    @DisplayName("Should return full payment timeline for PAID and AUTO_MATCHED invoice")
    void shouldReturnPaidPaymentStatus() {
        UUID invoiceId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice(tenant, "GIB2026000000001", "ettn-1", LocalDate.now().minusDays(10),
                InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA, null, vendor, legalEntity, costCenter,
                "TRY", new BigDecimal("10000.00"), new BigDecimal("2000.00"), BigDecimal.ZERO,
                new BigDecimal("12000.00"), new BigDecimal("12000.00"), MatchType.THREE_WAY);
        invoice.setId(invoiceId);
        invoice.setMatchStatus(InvoiceMatchStatus.AUTO_MATCHED);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setDueDate(LocalDate.now().plusDays(20));

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(supplierInvoiceRepository.findByIdAndTenantIdAndVendorId(invoiceId, tenantId, vendorId))
                .thenReturn(Optional.of(invoice));

        InvoicePaymentStatusResponse response = financeService.getInvoicePaymentStatus(invoiceId, vendorUserId);

        assertThat(response).isNotNull();
        assertThat(response.invoiceNumber()).isEqualTo("GIB2026000000001");
        assertThat(response.status()).isEqualTo("PAID");
        assertThat(response.matchStatus()).isEqualTo("AUTO_MATCHED");
        assertThat(response.bankReferenceNumber()).contains("TXN-TR-GARANTI-");
        assertThat(response.maskedIban()).isNotBlank();
        assertThat(response.timeline()).hasSize(4);
        assertThat(response.timeline().get(0).completed()).isTrue();
        assertThat(response.timeline().get(1).completed()).isTrue(); // MATCHED
        assertThat(response.timeline().get(2).completed()).isTrue(); // APPROVED
        assertThat(response.timeline().get(3).completed()).isTrue(); // PAID
    }

    @Test
    @DisplayName("Should return timeline for SUBMITTED and EVALUATING invoice with pending steps")
    void shouldReturnSubmittedPaymentStatus() {
        UUID invoiceId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice(tenant, "GIB2026000000002", "ettn-2", LocalDate.now(),
                InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA, null, vendor, legalEntity, costCenter,
                "TRY", new BigDecimal("5000.00"), new BigDecimal("1000.00"), BigDecimal.ZERO,
                new BigDecimal("6000.00"), new BigDecimal("6000.00"), MatchType.THREE_WAY);
        invoice.setId(invoiceId);
        invoice.setMatchStatus(InvoiceMatchStatus.EVALUATING);
        invoice.setStatus(InvoiceStatus.SUBMITTED);

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(supplierInvoiceRepository.findByIdAndTenantIdAndVendorId(invoiceId, tenantId, vendorId))
                .thenReturn(Optional.of(invoice));

        InvoicePaymentStatusResponse response = financeService.getInvoicePaymentStatus(invoiceId, vendorUserId);

        assertThat(response.status()).isEqualTo("SUBMITTED");
        assertThat(response.bankReferenceNumber()).isNull();
        assertThat(response.timeline().get(1).completed()).isFalse(); // Not matched
        assertThat(response.timeline().get(2).completed()).isFalse(); // Not approved
        assertThat(response.timeline().get(3).completed()).isFalse(); // Not paid
    }

    @Test
    @DisplayName("Should throw 404 when invoice not found for vendor")
    void shouldThrowWhenInvoiceNotFound() {
        UUID invoiceId = UUID.randomUUID();
        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(supplierInvoiceRepository.findByIdAndTenantIdAndVendorId(invoiceId, tenantId, vendorId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> financeService.getInvoicePaymentStatus(invoiceId, vendorUserId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invoice not found");
    }

    // =========================================================================
    // 2. getAvailableEarlyPaymentOffers & acceptEarlyPaymentOffer
    // =========================================================================

    @Test
    @DisplayName("Should generate dynamic 2% early pay offer when no existing offer exists")
    void shouldGenerateDynamicEarlyPayOffer() {
        UUID invoiceId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice(tenant, "GIB2026000000003", "ettn-3", LocalDate.now(),
                InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA, null, vendor, legalEntity, costCenter,
                "TRY", new BigDecimal("100000.00"), new BigDecimal("20000.00"), BigDecimal.ZERO,
                new BigDecimal("120000.00"), new BigDecimal("120000.00"), MatchType.THREE_WAY);
        invoice.setId(invoiceId);
        invoice.setStatus(InvoiceStatus.APPROVED_FOR_PAYMENT);
        invoice.setDueDate(LocalDate.now().plusDays(30));

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(supplierInvoiceRepository.findAllByTenantIdAndVendorIdAndStatusOrderByCreatedAtDesc(tenantId, vendorId, InvoiceStatus.APPROVED_FOR_PAYMENT))
                .thenReturn(List.of(invoice));
        when(earlyPayOfferRepository.findByTenantIdAndSupplierInvoiceId(tenantId, invoiceId)).thenReturn(Optional.empty());

        VendorEarlyPayOffer savedOffer = new VendorEarlyPayOffer(tenant, invoice, vendor,
                new BigDecimal("120000.00"), LocalDate.now().plusDays(30), new BigDecimal("2.00"),
                new BigDecimal("2400.00"), new BigDecimal("117600.00"), LocalDate.now().plusDays(3));
        savedOffer.setId(UUID.randomUUID());

        when(earlyPayOfferRepository.save(any())).thenReturn(savedOffer);

        List<EarlyPayOfferResponse> offers = financeService.getAvailableEarlyPaymentOffers(vendorUserId);

        assertThat(offers).hasSize(1);
        assertThat(offers.get(0).invoiceNumber()).isEqualTo("GIB2026000000003");
        assertThat(offers.get(0).discountPercentage()).isEqualByComparingTo("2.00");
        assertThat(offers.get(0).status()).isEqualTo("OFFERED");
    }

    @Test
    @DisplayName("Should accept active early payment offer and accelerate invoice payout")
    void shouldAcceptEarlyPaymentOffer() {
        UUID invoiceId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice(tenant, "GIB2026000000004", "ettn-4", LocalDate.now(),
                InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA, null, vendor, legalEntity, costCenter,
                "TRY", new BigDecimal("50000.00"), new BigDecimal("10000.00"), BigDecimal.ZERO,
                new BigDecimal("60000.00"), new BigDecimal("60000.00"), MatchType.THREE_WAY);
        invoice.setId(invoiceId);

        VendorEarlyPayOffer offer = new VendorEarlyPayOffer(tenant, invoice, vendor,
                new BigDecimal("60000.00"), LocalDate.now().plusDays(30), new BigDecimal("2.00"),
                new BigDecimal("1200.00"), new BigDecimal("58800.00"), LocalDate.now().plusDays(3));
        offer.setId(UUID.randomUUID());

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(supplierInvoiceRepository.findByIdAndTenantIdAndVendorId(invoiceId, tenantId, vendorId))
                .thenReturn(Optional.of(invoice));
        when(earlyPayOfferRepository.findByTenantIdAndSupplierInvoiceIdAndStatus(tenantId, invoiceId, EarlyPayOfferStatus.OFFERED))
                .thenReturn(Optional.of(offer));

        AcceptEarlyDiscountResponse response = financeService.acceptEarlyPaymentOffer(invoiceId, vendorUserId);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("ACCEPTED");
        assertThat(response.netPayoutAmount()).isEqualByComparingTo("58800.00");
        assertThat(invoice.getPayableAmount()).isEqualByComparingTo("58800.00");
        verify(earlyPayOfferRepository).save(offer);
        verify(supplierInvoiceRepository).save(invoice);
        verify(auditService).recordAuditLog(any());
    }

    @Test
    @DisplayName("Should throw 400 when no active early payment offer exists to accept")
    void shouldThrowWhenNoActiveEarlyPayOffer() {
        UUID invoiceId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice(tenant, "GIB2026000000005", "ettn-5", LocalDate.now(),
                InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA, null, vendor, legalEntity, costCenter,
                "TRY", new BigDecimal("10000.00"), new BigDecimal("2000.00"), BigDecimal.ZERO,
                new BigDecimal("12000.00"), new BigDecimal("12000.00"), MatchType.THREE_WAY);

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(supplierInvoiceRepository.findByIdAndTenantIdAndVendorId(invoiceId, tenantId, vendorId))
                .thenReturn(Optional.of(invoice));
        when(earlyPayOfferRepository.findByTenantIdAndSupplierInvoiceIdAndStatus(tenantId, invoiceId, EarlyPayOfferStatus.OFFERED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> financeService.acceptEarlyPaymentOffer(invoiceId, vendorUserId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No active early payment offer found");
    }

    // =========================================================================
    // 3. getStatementOfAccounts
    // =========================================================================

    @Test
    @DisplayName("Should compute Statement of Accounts with paid and unpaid invoices")
    void shouldGetStatementOfAccounts() {
        SupplierInvoice invPaid = new SupplierInvoice(tenant, "INV-PAID", "ettn-p", LocalDate.of(2026, 1, 15),
                InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA, null, vendor, legalEntity, costCenter,
                "TRY", new BigDecimal("10000.00"), new BigDecimal("2000.00"), BigDecimal.ZERO,
                new BigDecimal("12000.00"), new BigDecimal("12000.00"), MatchType.THREE_WAY);
        invPaid.setStatus(InvoiceStatus.PAID);

        SupplierInvoice invOpen = new SupplierInvoice(tenant, "INV-OPEN", "ettn-o", LocalDate.of(2026, 2, 10),
                InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA, null, vendor, legalEntity, costCenter,
                "TRY", new BigDecimal("20000.00"), new BigDecimal("4000.00"), BigDecimal.ZERO,
                new BigDecimal("24000.00"), new BigDecimal("24000.00"), MatchType.THREE_WAY);
        invOpen.setStatus(InvoiceStatus.APPROVED_FOR_PAYMENT);

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(supplierInvoiceRepository.findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(tenantId, vendorId))
                .thenReturn(List.of(invPaid, invOpen));

        StatementOfAccountsResponse soa = financeService.getStatementOfAccounts(null, null, vendorUserId);

        assertThat(soa).isNotNull();
        assertThat(soa.totalInvoiced()).isEqualByComparingTo("36000.00");
        assertThat(soa.totalPaid()).isEqualByComparingTo("12000.00");
        assertThat(soa.openBalance()).isEqualByComparingTo("24000.00");
        assertThat(soa.entries()).hasSize(2);
    }

    // =========================================================================
    // 4. Monthly Reconciliation (BA-BS)
    // =========================================================================

    @Test
    @DisplayName("Should get or create monthly reconciliation on the fly")
    void shouldGetMonthlyReconciliation() {
        SupplierInvoice inv = new SupplierInvoice(tenant, "INV-M1", "ettn-m1", LocalDate.of(2026, 8, 10),
                InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA, null, vendor, legalEntity, costCenter,
                "TRY", new BigDecimal("10000.00"), new BigDecimal("2000.00"), BigDecimal.ZERO,
                new BigDecimal("12000.00"), new BigDecimal("12000.00"), MatchType.THREE_WAY);

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(reconciliationRepository.findByTenantIdAndVendorIdAndPeriodYearAndPeriodMonth(tenantId, vendorId, 2026, 8))
                .thenReturn(Optional.empty());
        when(supplierInvoiceRepository.findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(tenantId, vendorId))
                .thenReturn(List.of(inv));

        VendorMonthlyReconciliation savedRec = new VendorMonthlyReconciliation(tenant, vendor, 2026, 8, 1, new BigDecimal("12000.00"));
        savedRec.setId(UUID.randomUUID());
        when(reconciliationRepository.save(any())).thenReturn(savedRec);

        MonthlyReconciliationResponse rec = financeService.getMonthlyReconciliation(2026, 8, vendorUserId);

        assertThat(rec).isNotNull();
        assertThat(rec.year()).isEqualTo(2026);
        assertThat(rec.month()).isEqualTo(8);
        assertThat(rec.invoiceCount()).isEqualTo(1);
        assertThat(rec.totalAmount()).isEqualByComparingTo("12000.00");
    }

    @Test
    @DisplayName("Should approve reconciliation with SHA-256 digital signature seal")
    void shouldApproveMonthlyReconciliation() {
        VendorMonthlyReconciliation rec = new VendorMonthlyReconciliation(tenant, vendor, 2026, 8, 2, new BigDecimal("25000.00"));
        rec.setId(UUID.randomUUID());

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(reconciliationRepository.findByTenantIdAndVendorIdAndPeriodYearAndPeriodMonth(tenantId, vendorId, 2026, 8))
                .thenReturn(Optional.of(rec));
        when(reconciliationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MonthlyReconciliationApprovalRequest request = new MonthlyReconciliationApprovalRequest(2026, 8, "Approved by CFO", false);
        MonthlyReconciliationResponse response = financeService.approveMonthlyReconciliation(request, vendorUserId);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.signedChecksum()).isNotBlank();
        verify(auditService).recordAuditLog(any());
    }

    @Test
    @DisplayName("Should dispute monthly reconciliation when requested")
    void shouldDisputeMonthlyReconciliation() {
        VendorMonthlyReconciliation rec = new VendorMonthlyReconciliation(tenant, vendor, 2026, 8, 2, new BigDecimal("25000.00"));
        rec.setId(UUID.randomUUID());

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(reconciliationRepository.findByTenantIdAndVendorIdAndPeriodYearAndPeriodMonth(tenantId, vendorId, 2026, 8))
                .thenReturn(Optional.of(rec));
        when(reconciliationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MonthlyReconciliationApprovalRequest request = new MonthlyReconciliationApprovalRequest(2026, 8, "Discrepancy on invoice 4", true);
        MonthlyReconciliationResponse response = financeService.approveMonthlyReconciliation(request, vendorUserId);

        assertThat(response.status()).isEqualTo("DISPUTED");
        assertThat(response.vendorNotes()).isEqualTo("Discrepancy on invoice 4");
        verify(auditService).recordAuditLog(any());
    }

    // =========================================================================
    // 5. Catalog Proposals
    // =========================================================================

    @Test
    @DisplayName("Should submit catalog proposal with and without item master link")
    void shouldSubmitCatalogProposal() {
        UUID itemMasterId = UUID.randomUUID();
        CatalogCategory cat = new CatalogCategory(tenant, null, "HW", "Hardware", null, null);
        CatalogItem item = new CatalogItem(tenant, "IT-001", "Server", "Dell Server", cat, vendor,
                new BigDecimal("50000.00"), "TRY", new BigDecimal("0.20"), "EA", null, null, null, false, null, null);
        item.setId(itemMasterId);

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(catalogItemRepository.findByTenantIdAndId(tenantId, itemMasterId)).thenReturn(Optional.of(item));

        VendorCatalogProposal proposal = new VendorCatalogProposal(tenant, vendor, item, "IT-001-V", "Dell Server v2",
                "Hardware", new BigDecimal("48000.00"), "TRY", new BigDecimal("20.00"), 5, "Special pricing");

        when(catalogProposalRepository.save(any())).thenAnswer(i -> {
            VendorCatalogProposal p = i.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
            return p;
        });

        VendorCatalogProposalRequest request = new VendorCatalogProposalRequest(itemMasterId, "IT-001-V", "Dell Server v2",
                "Hardware", new BigDecimal("48000.00"), "TRY", new BigDecimal("20.00"), 5, "Special pricing");

        VendorCatalogProposalResponse response = financeService.submitCatalogProposal(request, vendorUserId);

        assertThat(response).isNotNull();
        assertThat(response.proposedItemCode()).isEqualTo("IT-001-V");
        assertThat(response.proposedUnitPrice()).isEqualByComparingTo("48000.00");
        verify(auditService).recordAuditLog(any());
    }

    @Test
    @DisplayName("Should get all vendor catalog proposals")
    void shouldGetVendorCatalogProposals() {
        VendorCatalogProposal proposal = new VendorCatalogProposal(tenant, vendor, null, "NEW-001", "New Product",
                "Office", new BigDecimal("150.00"), "TRY", new BigDecimal("20.00"), 3, "New item");

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(catalogProposalRepository.findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(tenantId, vendorId))
                .thenReturn(List.of(proposal));

        List<VendorCatalogProposalResponse> proposals = financeService.getVendorCatalogProposals(vendorUserId);

        assertThat(proposals).hasSize(1);
        assertThat(proposals.get(0).proposedName()).isEqualTo("New Product");
    }
}
