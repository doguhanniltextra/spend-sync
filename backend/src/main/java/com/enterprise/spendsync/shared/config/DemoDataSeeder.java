package com.enterprise.spendsync.shared.config;

import com.enterprise.spendsync.budget.internal.domain.BudgetEnforcementMode;
import com.enterprise.spendsync.budget.internal.domain.BudgetPeriodType;
import com.enterprise.spendsync.budget.internal.domain.BudgetPool;
import com.enterprise.spendsync.budget.internal.domain.BudgetStatus;
import com.enterprise.spendsync.budget.internal.repository.BudgetPoolRepository;
import com.enterprise.spendsync.core.internal.domain.*;
import com.enterprise.spendsync.core.internal.repository.*;
import com.enterprise.spendsync.matching.internal.domain.*;
import com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceRepository;
import com.enterprise.spendsync.purchasing.internal.domain.*;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderRepository;
import com.enterprise.spendsync.purchasing.internal.repository.VendorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@Order(10)
public class DemoDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final FacilityRepository facilityRepository;
    private final CostCenterRepository costCenterRepository;
    private final BudgetPoolRepository budgetPoolRepository;
    private final VendorRepository vendorRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public DemoDataSeeder(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            LegalEntityRepository legalEntityRepository,
            FacilityRepository facilityRepository,
            CostCenterRepository costCenterRepository,
            BudgetPoolRepository budgetPoolRepository,
            VendorRepository vendorRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            SupplierInvoiceRepository supplierInvoiceRepository,
            PasswordEncoder passwordEncoder,
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate
    ) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.legalEntityRepository = legalEntityRepository;
        this.facilityRepository = facilityRepository;
        this.costCenterRepository = costCenterRepository;
        this.budgetPoolRepository = budgetPoolRepository;
        this.vendorRepository = vendorRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierInvoiceRepository = supplierInvoiceRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        ensureSchemaAligned();
        seedCfoDemoData();
    }

    private void ensureSchemaAligned() {
        try {
            jdbcTemplate.execute("ALTER TABLE supplier_invoices ADD COLUMN IF NOT EXISTS withholding_tax_amount NUMERIC(18, 4) DEFAULT 0;");
            jdbcTemplate.execute("ALTER TABLE supplier_invoices ADD COLUMN IF NOT EXISTS payable_amount NUMERIC(18, 4);");
            jdbcTemplate.execute("ALTER TABLE supplier_invoices ADD COLUMN IF NOT EXISTS exchange_rate NUMERIC(15, 6) DEFAULT 1.0;");
            jdbcTemplate.execute("ALTER TABLE supplier_invoices ADD COLUMN IF NOT EXISTS match_type VARCHAR(50) DEFAULT 'THREE_WAY';");
            jdbcTemplate.execute("ALTER TABLE supplier_invoices ADD COLUMN IF NOT EXISTS raw_ubl_xml TEXT;");
            jdbcTemplate.execute("ALTER TABLE supplier_invoices ADD COLUMN IF NOT EXISTS due_date DATE;");
            jdbcTemplate.execute("ALTER TABLE supplier_invoices ADD COLUMN IF NOT EXISTS rejection_reason TEXT;");
            jdbcTemplate.execute("ALTER TABLE supplier_invoices DROP CONSTRAINT IF EXISTS supplier_invoices_match_status_check;");
            jdbcTemplate.execute("ALTER TABLE supplier_invoices DROP CONSTRAINT IF EXISTS supplier_invoices_status_check;");

            jdbcTemplate.execute("ALTER TABLE supplier_invoice_line_items ADD COLUMN IF NOT EXISTS tevkifat_code VARCHAR(20);");
            jdbcTemplate.execute("ALTER TABLE supplier_invoice_line_items ADD COLUMN IF NOT EXISTS tevkifat_rate VARCHAR(20);");
            jdbcTemplate.execute("ALTER TABLE supplier_invoice_line_items ADD COLUMN IF NOT EXISTS tevkifat_amount NUMERIC(18, 4) DEFAULT 0;");
            jdbcTemplate.execute("ALTER TABLE supplier_invoice_line_items ADD COLUMN IF NOT EXISTS line_total_amount NUMERIC(18, 4);");
            jdbcTemplate.execute("ALTER TABLE supplier_invoice_line_items DROP CONSTRAINT IF EXISTS supplier_invoice_line_items_match_status_check;");

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS invoice_discrepancies (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    tenant_id UUID NOT NULL,
                    supplier_invoice_id UUID NOT NULL,
                    discrepancy_type VARCHAR(50) NOT NULL,
                    expected_value VARCHAR(100),
                    actual_value VARCHAR(100),
                    variance_amount NUMERIC(18, 4),
                    variance_percentage NUMERIC(6, 2),
                    resolved BOOLEAN NOT NULL DEFAULT FALSE,
                    resolution_notes TEXT,
                    resolved_by_user_id UUID,
                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                );
            """);

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS vendor_early_pay_offers (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    tenant_id UUID NOT NULL,
                    supplier_invoice_id UUID NOT NULL,
                    vendor_id UUID NOT NULL,
                    original_amount NUMERIC(18, 4) NOT NULL,
                    original_due_date DATE NOT NULL,
                    discount_percentage NUMERIC(5, 2) NOT NULL,
                    discount_amount NUMERIC(18, 4) NOT NULL,
                    net_payout_amount NUMERIC(18, 4) NOT NULL,
                    accelerated_payment_date DATE NOT NULL,
                    status VARCHAR(50) NOT NULL DEFAULT 'OFFERED',
                    accepted_at TIMESTAMPTZ,
                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                );
            """);

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS vendor_catalog_proposals (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    tenant_id UUID NOT NULL,
                    vendor_id UUID NOT NULL,
                    item_master_id UUID,
                    proposed_item_code VARCHAR(100) NOT NULL,
                    proposed_name VARCHAR(255) NOT NULL,
                    proposed_category VARCHAR(100) NOT NULL,
                    proposed_unit_price NUMERIC(18, 4) NOT NULL,
                    proposed_currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
                    vat_rate NUMERIC(5, 2) NOT NULL DEFAULT 20.00,
                    lead_time_days INTEGER NOT NULL DEFAULT 3,
                    notes TEXT,
                    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_BUYER_REVIEW',
                    buyer_decision_notes TEXT,
                    reviewed_by_user_id UUID,
                    reviewed_at TIMESTAMPTZ,
                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                );
            """);

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS vendor_monthly_reconciliations (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    tenant_id UUID NOT NULL,
                    vendor_id UUID NOT NULL,
                    period_year INTEGER NOT NULL,
                    period_month INTEGER NOT NULL,
                    invoice_count INTEGER NOT NULL,
                    total_amount NUMERIC(18, 4) NOT NULL,
                    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                    vendor_approved_at TIMESTAMPTZ,
                    vendor_notes TEXT,
                    signed_checksum VARCHAR(64),
                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    CONSTRAINT uk_reconciliation_period UNIQUE (tenant_id, vendor_id, period_year, period_month)
                );
            """);
        } catch (Exception e) {
            System.err.println("Schema alignment notice: " + e.getMessage());
        }
    }

    private void seedCfoDemoData() {
        String cfoEmail = "cfo@spendsync.com";
        Optional<User> existingUser = userRepository.findByEmail(cfoEmail);

        User cfoUser;
        Tenant tenant;

        if (existingUser.isEmpty()) {
            tenant = tenantRepository.findBySlug("spendsync-corp")
                    .orElseGet(() -> tenantRepository.save(new Tenant("SpendSync Global Enterprise", "spendsync-corp")));

            cfoUser = new User();
            cfoUser.setEmail(cfoEmail);
            cfoUser.setPasswordHash(passwordEncoder.encode("Password123!"));
            cfoUser.setFirstName("Sarah");
            cfoUser.setLastName("Jenkins");
            cfoUser.setJobTitle("Chief Financial Officer");
            cfoUser.setRoles(Set.of(RoleType.ROOT_USER));
            cfoUser.setTenant(tenant);
            cfoUser.setCountry("TR");
            cfoUser.setTimezone("Europe/Istanbul");
            cfoUser.setPreferredLanguage("en");
            cfoUser = userRepository.save(cfoUser);
        } else {
            cfoUser = existingUser.get();
            if (cfoUser.getTenant() == null) {
                tenant = tenantRepository.findBySlug("spendsync-corp")
                        .orElseGet(() -> tenantRepository.save(new Tenant("SpendSync Global Enterprise", "spendsync-corp")));
                cfoUser.setTenant(tenant);
                cfoUser = userRepository.save(cfoUser);
            } else {
                tenant = cfoUser.getTenant();
            }
        }

        // Check if data already seeded for this tenant
        List<BudgetPool> existingPools = budgetPoolRepository.findAllByTenantId(tenant.getId());
        if (!existingPools.isEmpty()) {
            return; // Already seeded
        }

        // 1. Legal Entity
        LegalEntity legalEntity = legalEntityRepository.save(new LegalEntity(
                tenant,
                "SpendSync Technology Holding Inc.",
                "LE-TR-01",
                "8822001144",
                "TRY",
                "Buyukdere Ave. No:199 Levent / Istanbul",
                "TR"
        ));

        // 2. Facilities
        Facility hqFacility = facilityRepository.save(new Facility(
                tenant,
                legalEntity,
                "Istanbul Maslak HQ Office",
                "FAC-HQ-01",
                FacilityType.OFFICE,
                "Maslak Financial Center Tower 2, Sariyer / Istanbul"
        ));

        Facility logisticsFacility = facilityRepository.save(new Facility(
                tenant,
                legalEntity,
                "Gebze R&D Logistics Hub",
                "FAC-LOG-01",
                FacilityType.WAREHOUSE,
                "GOSB Teknopark Cad. No:12 Gebze / Kocaeli"
        ));

        // 3. Cost Centers
        CostCenter ccIt = costCenterRepository.save(new CostCenter(tenant, legalEntity, "CC-IT-100", "IT Infrastructure & Cloud"));
        CostCenter ccRd = costCenterRepository.save(new CostCenter(tenant, legalEntity, "CC-RD-200", "Software Engineering & R&D"));
        CostCenter ccOps = costCenterRepository.save(new CostCenter(tenant, legalEntity, "CC-OPS-300", "Logistics & Workplace Operations"));
        CostCenter ccMkt = costCenterRepository.save(new CostCenter(tenant, legalEntity, "CC-MKT-400", "Global Marketing & Growth"));

        // 4. Budget Pools (Total 26.000.000 TRY)
        BudgetPool poolIt = new BudgetPool(
                tenant, legalEntity, ccIt, 2026, BudgetPeriodType.ANNUAL, "2026",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO,
                new BigDecimal("8000000.00"), "TRY"
        );
        poolIt.setSpentAmount(new BigDecimal("3800000.00"));
        poolIt.setReservedAmount(new BigDecimal("950000.00"));
        budgetPoolRepository.save(poolIt);

        BudgetPool poolRd = new BudgetPool(
                tenant, legalEntity, ccRd, 2026, BudgetPeriodType.ANNUAL, "2026",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO,
                new BigDecimal("10000000.00"), "TRY"
        );
        poolRd.setSpentAmount(new BigDecimal("4200000.00"));
        poolRd.setReservedAmount(new BigDecimal("1400000.00"));
        budgetPoolRepository.save(poolRd);

        BudgetPool poolOps = new BudgetPool(
                tenant, legalEntity, ccOps, 2026, BudgetPeriodType.ANNUAL, "2026",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO,
                new BigDecimal("5000000.00"), "TRY"
        );
        poolOps.setSpentAmount(new BigDecimal("2100000.00"));
        poolOps.setReservedAmount(new BigDecimal("600000.00"));
        budgetPoolRepository.save(poolOps);

        BudgetPool poolMkt = new BudgetPool(
                tenant, legalEntity, ccMkt, 2026, BudgetPeriodType.ANNUAL, "2026",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO,
                new BigDecimal("3000000.00"), "TRY"
        );
        poolMkt.setSpentAmount(new BigDecimal("950000.00"));
        poolMkt.setReservedAmount(new BigDecimal("250000.00"));
        budgetPoolRepository.save(poolMkt);

        // 5. Vendors
        Vendor vAws = vendorRepository.save(new Vendor(
                tenant, "Amazon Web Services EMEA SARL", "LU12000000", "Maslak",
                VendorCategory.SOFTWARE_SAAS, VendorTier.TIER_1_STRATEGIC, true,
                "orders@aws-emea.com", "+3521234567", "38 Avenue John F. Kennedy", "Luxembourg", "LU",
                PaymentTerms.NET_30, "BNP Paribas", "LU280012345678901234"
        ));

        Vendor vApple = vendorRepository.save(new Vendor(
                tenant, "Apple Computer International Ltd.", "8844112233", "Large Taxpayers Office",
                VendorCategory.IT_HARDWARE, VendorTier.TIER_1_STRATEGIC, true,
                "b2b@apple.com", "+902123456789", "Levent 199, Istanbul", "Istanbul", "TR",
                PaymentTerms.NET_30, "Garanti BBVA", "TR330006200000012345678901"
        ));

        Vendor vDatadog = vendorRepository.save(new Vendor(
                tenant, "Datadog Europe B.V.", "NL8822991100", "Maslak",
                VendorCategory.SOFTWARE_SAAS, VendorTier.TIER_2_PREFERRED, true,
                "billing@datadoghq.com", "+31201234567", "Keizersgracht 421", "Amsterdam", "NL",
                PaymentTerms.NET_30, "ING Bank", "NL91INGB0001234567"
        ));

        Vendor vInsight = vendorRepository.save(new Vendor(
                tenant, "Insight Direct Enterprises Corp.", "9911223344", "Zincirlikuyu",
                VendorCategory.IT_HARDWARE, VendorTier.TIER_3_STANDARD, true,
                "orders@insight.com", "+902129876543", "Zincirlikuyu Plaza No:4", "Istanbul", "TR",
                PaymentTerms.NET_30, "Isbank", "TR120006400000112233445566"
        ));

        Vendor vOfisline = vendorRepository.save(new Vendor(
                tenant, "Ofisline Supplies & Logistics Inc.", "3344556677", "Kadikoy",
                VendorCategory.OFFICE_SUPPLIES, VendorTier.TIER_3_STANDARD, true,
                "orders@ofisline.com.tr", "+902165554433", "Kozyatagi St. Degirmen Ave.", "Istanbul", "TR",
                PaymentTerms.NET_15, "Akbank", "TR550004600000998877665544"
        ));

        // 6. Purchase Orders
        // PO 1: AWS
        PurchaseOrder poAws = new PurchaseOrder(
                tenant, "PO-2026-0001", null, legalEntity, ccIt, hqFacility, vAws,
                Incoterms.DAP, "TRY", PaymentTerms.NET_30, "Annual AWS Cloud Infrastructure Hosting", cfoUser
        );
        poAws.addLineItem(new PurchaseOrderLineItem(
                tenant, poAws, null, 1, "AWS EC2 & Kubernetes Cloud Compute", "SOFTWARE_SAAS",
                BigDecimal.ONE, "MONTH", new BigDecimal("3540000.00"), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now().plusMonths(1)
        ));
        poAws.setStatus(PurchaseOrderStatus.ISSUED);
        purchaseOrderRepository.save(poAws);

        // PO 2: Apple
        PurchaseOrder poApple = new PurchaseOrder(
                tenant, "PO-2026-0002", null, legalEntity, ccRd, hqFacility, vApple,
                Incoterms.DDP, "TRY", PaymentTerms.NET_30, "Engineering Fleet Hardware Refresh", cfoUser
        );
        poApple.addLineItem(new PurchaseOrderLineItem(
                tenant, poApple, null, 1, "MacBook Pro 16\" M3 Max (32GB / 1TB)", "IT_HARDWARE",
                new BigDecimal("25"), "PIECE", new BigDecimal("110000.00"), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now().plusDays(15)
        ));
        poApple.setStatus(PurchaseOrderStatus.ISSUED);
        purchaseOrderRepository.save(poApple);

        // PO 3: Datadog
        PurchaseOrder poDatadog = new PurchaseOrder(
                tenant, "PO-2026-0003", null, legalEntity, ccIt, hqFacility, vDatadog,
                Incoterms.DAP, "TRY", PaymentTerms.NET_30, "Datadog APM & Logs License", cfoUser
        );
        poDatadog.addLineItem(new PurchaseOrderLineItem(
                tenant, poDatadog, null, 1, "Datadog Infrastructure Monitoring Pro", "SOFTWARE_SAAS",
                BigDecimal.ONE, "YEAR", new BigDecimal("1420000.00"), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now().plusMonths(2)
        ));
        poDatadog.setStatus(PurchaseOrderStatus.ISSUED);
        purchaseOrderRepository.save(poDatadog);

        // PO 4: Insight
        PurchaseOrder poInsight = new PurchaseOrder(
                tenant, "PO-2026-0004", null, legalEntity, ccOps, logisticsFacility, vInsight,
                Incoterms.DAP, "TRY", PaymentTerms.NET_30, "Gebze Logistics Server Racks", cfoUser
        );
        poInsight.addLineItem(new PurchaseOrderLineItem(
                tenant, poInsight, null, 1, "Dell PowerEdge R760 Server", "IT_HARDWARE",
                new BigDecimal("4"), "PIECE", new BigDecimal("222500.00"), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now().plusMonths(3)
        ));
        poInsight.setStatus(PurchaseOrderStatus.ISSUED);
        purchaseOrderRepository.save(poInsight);

        // PO 5: Ofisline
        PurchaseOrder poOfis = new PurchaseOrder(
                tenant, "PO-2026-0005", null, legalEntity, ccOps, hqFacility, vOfisline,
                Incoterms.DDP, "TRY", PaymentTerms.NET_15, "Office Ergonomic Workstations", cfoUser
        );
        poOfis.addLineItem(new PurchaseOrderLineItem(
                tenant, poOfis, null, 1, "Herman Miller Ergonomic Chairs", "OFFICE_SUPPLIES",
                new BigDecimal("20"), "PIECE", new BigDecimal("24000.00"), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now().plusDays(20)
        ));
        poOfis.setStatus(PurchaseOrderStatus.ISSUED);
        purchaseOrderRepository.save(poOfis);

        // 7. Supplier Invoices (Spread over Aug, Sep, Oct, Nov)
        LocalDate now = LocalDate.now();

        // Invoice 1: AWS (Aug 2026 due)
        SupplierInvoice inv1 = new SupplierInvoice(
                tenant, "INV-AWS-2026-08", "ETTN-AWS-001", now.minusDays(10),
                InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA, poAws, vAws, legalEntity, ccIt,
                "TRY", new BigDecimal("1200000.00"), new BigDecimal("240000.00"), new BigDecimal("1440000.00")
        );
        inv1.setMatchStatus(InvoiceMatchStatus.AUTO_MATCHED);
        inv1.setStatus(InvoiceStatus.APPROVED_FOR_PAYMENT);
        supplierInvoiceRepository.save(inv1);

        // Invoice 2: Apple (Sep 2026 due)
        SupplierInvoice inv2 = new SupplierInvoice(
                tenant, "INV-APL-2026-09", "ETTN-APL-002", now.plusDays(5),
                InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA, poApple, vApple, legalEntity, ccRd,
                "TRY", new BigDecimal("1100000.00"), new BigDecimal("220000.00"), new BigDecimal("1320000.00")
        );
        inv2.setMatchStatus(InvoiceMatchStatus.AUTO_MATCHED);
        inv2.setStatus(InvoiceStatus.APPROVED_FOR_PAYMENT);
        supplierInvoiceRepository.save(inv2);

        // Invoice 3: Datadog (Oct 2026 due)
        SupplierInvoice inv3 = new SupplierInvoice(
                tenant, "INV-DDG-2026-10", "ETTN-DDG-003", now.plusDays(35),
                InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA, poDatadog, vDatadog, legalEntity, ccIt,
                "TRY", new BigDecimal("700000.00"), new BigDecimal("140000.00"), new BigDecimal("840000.00")
        );
        inv3.setMatchStatus(InvoiceMatchStatus.AUTO_MATCHED);
        inv3.setStatus(InvoiceStatus.APPROVED_FOR_PAYMENT);
        supplierInvoiceRepository.save(inv3);

        // Invoice 4: Insight (Nov 2026 due)
        SupplierInvoice inv4 = new SupplierInvoice(
                tenant, "INV-INS-2026-11", "ETTN-INS-004", now.plusDays(65),
                InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA, poInsight, vInsight, legalEntity, ccOps,
                "TRY", new BigDecimal("890000.00"), new BigDecimal("178000.00"), new BigDecimal("1068000.00")
        );
        inv4.setMatchStatus(InvoiceMatchStatus.AUTO_MATCHED);
        inv4.setStatus(InvoiceStatus.APPROVED_FOR_PAYMENT);
        supplierInvoiceRepository.save(inv4);

        // Invoice 5: Discrepancy Hold Invoice
        SupplierInvoice inv5 = new SupplierInvoice(
                tenant, "INV-AWS-DISC-01", "ETTN-AWS-DISC", now,
                InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA, poAws, vAws, legalEntity, ccIt,
                "TRY", new BigDecimal("350000.00"), new BigDecimal("70000.00"), new BigDecimal("420000.00")
        );
        inv5.setMatchStatus(InvoiceMatchStatus.DISCREPANCY_HOLD);
        inv5.setStatus(InvoiceStatus.SUBMITTED);
        inv5.setDiscrepancyReason("Unit price variance detected: Invoiced unit price exceeds PO negotiated rate by 12%");
        supplierInvoiceRepository.save(inv5);
    }
}
