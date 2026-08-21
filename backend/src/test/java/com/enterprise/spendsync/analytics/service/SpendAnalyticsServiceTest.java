package com.enterprise.spendsync.analytics.service;

import com.enterprise.spendsync.analytics.dto.CfoExecutiveDeckResponse;
import com.enterprise.spendsync.analytics.internal.service.CfoAnalyticsServiceImpl;
import com.enterprise.spendsync.budget.internal.domain.BudgetEnforcementMode;
import com.enterprise.spendsync.budget.internal.domain.BudgetPeriodType;
import com.enterprise.spendsync.budget.internal.domain.BudgetPool;
import com.enterprise.spendsync.budget.internal.domain.BudgetStatus;
import com.enterprise.spendsync.budget.internal.repository.BudgetPoolRepository;
import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.matching.internal.domain.InvoiceMatchStatus;
import com.enterprise.spendsync.matching.internal.domain.InvoiceProfile;
import com.enterprise.spendsync.matching.internal.domain.InvoiceStatus;
import com.enterprise.spendsync.matching.internal.domain.InvoiceType;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoice;
import com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceRepository;
import com.enterprise.spendsync.purchasing.internal.domain.*;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderRepository;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CFO Spend Analytics & Executive Deck Service Tests")
class SpendAnalyticsServiceTest {

    @Mock
    private BudgetPoolRepository budgetPoolRepository;
    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private SupplierInvoiceRepository supplierInvoiceRepository;

    private CfoAnalyticsServiceImpl analyticsService;
    private UUID tenantId;
    private Tenant tenant;
    private CostCenter costCenter;
    private Vendor vendor;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("SpendSync Enterprise");

        LegalEntity legalEntity = new LegalEntity(tenant, "SpendSync TR", "TR01", "1234567890", "TRY", "Istanbul", "TR");
        legalEntity.setId(UUID.randomUUID());

        costCenter = new CostCenter(tenant, legalEntity, "CC-ENG", "Software Engineering");
        costCenter.setId(UUID.randomUUID());

        vendor = new Vendor(
                tenant, "AWS EMEA SARL", "1112223334", "Maslak",
                VendorCategory.SOFTWARE_SAAS, VendorTier.TIER_1_STRATEGIC, true,
                "aws@amazon.com", "+90 212 111 2233", "Maslak", "Istanbul", "TR",
                PaymentTerms.NET_30, "Garanti BBVA", "TR330006200000012345678901"
        );
        vendor.setId(UUID.randomUUID());

        analyticsService = new CfoAnalyticsServiceImpl(budgetPoolRepository, purchaseOrderRepository, supplierInvoiceRepository);
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("TC-10-04, TC-10-05, TC-10-06: Aggregates budget utilization, category distribution, vendor concentration and 3-way match integrity")
    void shouldAggregateCfoExecutiveDeckMetrics() {
        // 1. Budget Pools: 1,000,000 Allocated, 400,000 Spent, 100,000 Reserved -> 50% Utilization
        BudgetPool pool = new BudgetPool(
                tenant, costCenter.getLegalEntity(), costCenter, 2026,
                BudgetPeriodType.ANNUAL, "ANNUAL", BudgetStatus.ACTIVE,
                BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO,
                new BigDecimal("1000000.00"), "TRY"
        );
        pool.setSpentAmount(new BigDecimal("400000.00"));
        pool.setReservedAmount(new BigDecimal("100000.00"));

        when(budgetPoolRepository.findAllByTenantId(tenantId)).thenReturn(List.of(pool));

        // 2. Active POs: 1 PO for Cloud Hosting (SAAS) = 250,000 TL
        User creator = new User("procurement@spendsync.com", "pass", "Proc", "User", null, "TR");
        creator.setId(UUID.randomUUID());

        PurchaseOrder po = new PurchaseOrder(
                tenant, "PO-2026-00001", null, costCenter.getLegalEntity(), costCenter, null,
                vendor, Incoterms.DAP, "TRY", PaymentTerms.NET_30, null, creator
        );
        po.setId(UUID.randomUUID());
        po.setStatus(PurchaseOrderStatus.ISSUED);

        PurchaseOrderLineItem poLine = new PurchaseOrderLineItem(
                tenant, po, null, 1, "AWS Cloud Cluster", "CLOUD_INFRASTRUCTURE",
                BigDecimal.ONE, "MONTH", new BigDecimal("250000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now().plusDays(30)
        );
        poLine.setId(UUID.randomUUID());
        po.addLineItem(poLine);

        when(purchaseOrderRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(List.of(po));

        // 3. Supplier Invoices: 1 Auto-Matched invoice
        SupplierInvoice invoice = new SupplierInvoice(
                tenant, "INV-2026-001", "ettn-001", LocalDate.now(),
                InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA,
                po, vendor, costCenter.getLegalEntity(), costCenter, "TRY",
                new BigDecimal("250000.00"), new BigDecimal("50000.00"), new BigDecimal("300000.00")
        );
        invoice.setId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.APPROVED_FOR_PAYMENT);
        invoice.setMatchStatus(InvoiceMatchStatus.AUTO_MATCHED);

        when(supplierInvoiceRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(List.of(invoice));

        CfoExecutiveDeckResponse deck = analyticsService.getCfoExecutiveDeck();

        assertThat(deck).isNotNull();
        // Budget Aggregates (TC-10-04)
        assertThat(deck.totalAllocatedBudget()).isEqualByComparingTo("1000000.00");
        assertThat(deck.totalSpendYtd()).isEqualByComparingTo("400000.00");
        assertThat(deck.overallBudgetUtilizationPercent()).isEqualTo(50.0);

        // Category Spend Distribution (TC-10-05)
        assertThat(deck.categoryDistribution()).hasSize(1);
        assertThat(deck.categoryDistribution().get(0).category()).isEqualTo("CLOUD_INFRASTRUCTURE");
        assertThat(deck.categoryDistribution().get(0).amount()).isEqualByComparingTo("250000.00");
        assertThat(deck.categoryDistribution().get(0).sharePercent()).isEqualTo(100.0);

        // Top Vendor Concentration (TC-10-05)
        assertThat(deck.topVendors()).hasSize(1);
        assertThat(deck.topVendors().get(0).vendorName()).isEqualTo("AWS EMEA SARL");
        assertThat(deck.topVendors().get(0).volume()).isEqualByComparingTo("250000.00");

        // 3-Way Match Integrity (TC-10-06)
        assertThat(deck.matchIntegrity()).isNotNull();
        assertThat(deck.matchIntegrity().firstTimeMatchRatePercent()).isEqualTo(100.0);
        assertThat(deck.matchIntegrity().discrepancyHoldInvoices()).isEqualTo(0);
    }
}
