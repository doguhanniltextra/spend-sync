package com.enterprise.spendsync.matching.service;

import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.matching.internal.domain.InvoiceProfile;
import com.enterprise.spendsync.matching.internal.domain.InvoiceType;
import com.enterprise.spendsync.matching.internal.domain.MatchType;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoice;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoiceLineItem;
import com.enterprise.spendsync.matching.internal.service.GibXsltRendererServiceImpl;
import com.enterprise.spendsync.purchasing.internal.domain.Incoterms;
import com.enterprise.spendsync.purchasing.internal.domain.PaymentTerms;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrder;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderLineItem;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import com.enterprise.spendsync.purchasing.internal.domain.VendorCategory;
import com.enterprise.spendsync.purchasing.internal.domain.VendorTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class GibXsltRendererServiceTest {

    private GibXsltRendererServiceImpl rendererService;

    private Tenant tenant;
    private LegalEntity legalEntity;
    private Vendor vendor;
    private PurchaseOrder purchaseOrder;

    @BeforeEach
    void setUp() {
        rendererService = new GibXsltRendererServiceImpl();

        tenant = new Tenant("Test Tenant", "test-tenant");
        legalEntity = new LegalEntity(tenant, "Buyer Corp A.Ş.", "LE-TR", "9876543210", "TRY", "Maslak, Istanbul", "TR");
        vendor = new Vendor(tenant, "Supplier Ltd. & Co.", "1234567890", "Karaköy", VendorCategory.IT_HARDWARE,
                VendorTier.TIER_1_STRATEGIC, true, "orders@supplier.com", "555-9999",
                "Perpa Ticaret Merkezi", "Istanbul", "TR", PaymentTerms.NET_30, "Garanti", "TR000000000");

        purchaseOrder = new PurchaseOrder(tenant, "PO-2026-00042", null, legalEntity,
                new CostCenter(tenant, legalEntity, "CC-01", "Finance"), null, vendor, Incoterms.DAP, "TRY", PaymentTerms.NET_30, "Notes", null);
    }

    @Test
    @DisplayName("Should render full HTML for electronic invoice with tevkifat line items")
    void shouldRenderFullInvoiceHtmlWithTevkifat() {
        SupplierInvoice invoice = new SupplierInvoice(
                tenant, "GIB2026000000001", "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                LocalDate.of(2026, 8, 20), InvoiceType.TEVKIFAT, InvoiceProfile.TEMEL_FATURA,
                purchaseOrder, vendor, legalEntity, null,
                "TRY", new BigDecimal("10000.00"), new BigDecimal("2000.00"),
                new BigDecimal("1400.00"), new BigDecimal("12000.00"), new BigDecimal("10600.00"),
                MatchType.THREE_WAY
        );

        PurchaseOrderLineItem poLine = new PurchaseOrderLineItem(
                tenant, purchaseOrder, null, 1, "Software License <Enterprise> & Maintenance", "IT",
                new BigDecimal("10"), "PIECE", new BigDecimal("1000.00"), null, null, null);

        SupplierInvoiceLineItem item1 = new SupplierInvoiceLineItem(
                tenant, poLine, null, new BigDecimal("10"), new BigDecimal("1000.00"),
                new BigDecimal("20.00"), new BigDecimal("2000.00"),
                "608", "7/10", new BigDecimal("1400.00"), new BigDecimal("10000.00")
        );
        invoice.addLineItem(item1);

        String html = rendererService.renderInvoiceHtml(invoice);

        assertThat(html).isNotNull();
        assertThat(html).contains("GIB2026000000001");
        assertThat(html).contains("TEMEL_FATURA");
        assertThat(html).contains("TEVKIFAT");
        assertThat(html).contains("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        assertThat(html).contains("PO-2026-00042");
        assertThat(html).contains("Buyer Corp A.Ş.");
        assertThat(html).contains("Supplier Ltd. &amp; Co.");
        assertThat(html).contains("Software License &lt;Enterprise&gt; &amp; Maintenance");
        assertThat(html).contains("608 (7/10)");
        assertThat(html).contains("PAYABLE TOTAL (NET)");
    }

    @Test
    @DisplayName("Should render invoice with null references and no tevkifat gracefully")
    void shouldRenderInvoiceWithNullReferences() {
        SupplierInvoice minimalInvoice = new SupplierInvoice(
                tenant, "GIB2026000000099", "ettn-uuid-99",
                LocalDate.of(2026, 8, 21), InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA,
                null, null, null, null,
                "USD", new BigDecimal("500.00"), new BigDecimal("100.00"),
                BigDecimal.ZERO, new BigDecimal("600.00"), new BigDecimal("600.00"),
                MatchType.THREE_WAY
        );

        SupplierInvoiceLineItem itemWithoutPo = new SupplierInvoiceLineItem(
                tenant, null, null, new BigDecimal("1"), new BigDecimal("500.00"),
                new BigDecimal("20.00"), new BigDecimal("100.00"), new BigDecimal("500.00")
        );
        minimalInvoice.addLineItem(itemWithoutPo);

        String html = rendererService.renderInvoiceHtml(minimalInvoice);

        assertThat(html).isNotNull();
        assertThat(html).contains("GIB2026000000099");
        assertThat(html).contains("TICARI_FATURA");
        assertThat(html).contains("SATIS");
        assertThat(html).contains("Product / Service");
        assertThat(html).contains("Vendor");
        assertThat(html).contains("Buyer Company");
        assertThat(html).contains("N/A");
    }
}
