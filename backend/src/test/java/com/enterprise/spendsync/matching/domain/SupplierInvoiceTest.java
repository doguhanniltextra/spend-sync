package com.enterprise.spendsync.matching.domain;

import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.matching.internal.domain.*;
import com.enterprise.spendsync.purchasing.internal.domain.Incoterms;
import com.enterprise.spendsync.purchasing.internal.domain.PaymentTerms;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrder;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderLineItem;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SupplierInvoice Domain Entity Pure Unit Tests")
class SupplierInvoiceTest {

    private Tenant tenant;
    private LegalEntity legalEntity;
    private CostCenter costCenter;
    private Vendor vendor;
    private User creator;
    private PurchaseOrder po;
    private PurchaseOrderLineItem poLine;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName("SpendSync Global");

        legalEntity = new LegalEntity(tenant, "SpendSync Turkey", "TR01", "1234567890", "TRY", "Istanbul", "TR");
        legalEntity.setId(UUID.randomUUID());

        costCenter = new CostCenter(tenant, legalEntity, "CC-IT", "IT Department");
        costCenter.setId(UUID.randomUUID());

        vendor = new Vendor();
        vendor.setId(UUID.randomUUID());
        vendor.setName("Global Server Supplies");

        creator = new User("ap@spendsync.com", "pass", "AP", "Officer", null, "TR");
        creator.setId(UUID.randomUUID());

        po = new PurchaseOrder(
                tenant, "PO-2026-00001", null, legalEntity, costCenter, null,
                vendor, Incoterms.DAP, "TRY", PaymentTerms.NET_30, null, creator
        );
        po.setId(UUID.randomUUID());

        poLine = new PurchaseOrderLineItem(
                tenant, po, null, 1, "Server Rack 42U", "IT_HARDWARE",
                new BigDecimal("10.0000"), "PIECE", new BigDecimal("10000.0000"),
                BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now().plusDays(10)
        );
        poLine.setId(UUID.randomUUID());
        po.addLineItem(poLine);
    }

    @Test
    @DisplayName("Should initialize Supplier Invoice with EVALUATING match status and SUBMITTED status")
    void shouldInitializeSupplierInvoiceDefaults() {
        SupplierInvoice invoice = new SupplierInvoice(
                tenant,
                "INV-2026-0001",
                "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                LocalDate.now(),
                InvoiceType.SATIS,
                InvoiceProfile.TICARI_FATURA,
                po,
                vendor,
                legalEntity,
                costCenter,
                "TRY",
                new BigDecimal("100000.0000"),
                new BigDecimal("20000.0000"),
                new BigDecimal("120000.0000")
        );

        SupplierInvoiceLineItem item = new SupplierInvoiceLineItem(
                tenant,
                poLine,
                null,
                new BigDecimal("10.0000"),
                new BigDecimal("10000.0000"),
                new BigDecimal("20.00"),
                new BigDecimal("20000.0000"),
                new BigDecimal("120000.0000")
        );
        invoice.addLineItem(item);

        assertThat(invoice.getMatchStatus()).isEqualTo(InvoiceMatchStatus.EVALUATING);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.SUBMITTED);
        assertThat(invoice.getInvoiceNumber()).isEqualTo("INV-2026-0001");
        assertThat(invoice.getEttn()).isEqualTo("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
        assertThat(invoice.getLineItems()).hasSize(1);
        assertThat(invoice.getLineItems().get(0).getSupplierInvoice()).isEqualTo(invoice);
    }
}
