package com.enterprise.spendsync.purchasing.domain;

import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.Facility;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.purchasing.internal.domain.Incoterms;
import com.enterprise.spendsync.purchasing.internal.domain.PaymentTerms;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrder;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderLineItem;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderStatus;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PurchaseOrder Domain Entity Pure Unit Tests")
class PurchaseOrderTest {

    private Tenant tenant;
    private LegalEntity legalEntity;
    private CostCenter costCenter;
    private Facility facility;
    private Vendor vendor;
    private User creator;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName("SpendSync Global");

        legalEntity = new LegalEntity(tenant, "SpendSync Turkey", "TR01", "1234567890", "TRY", "Istanbul", "TR");
        legalEntity.setId(UUID.randomUUID());

        costCenter = new CostCenter(tenant, legalEntity, "CC-IT-01", "Engineering");
        costCenter.setId(UUID.randomUUID());

        facility = new Facility(tenant, legalEntity, "Headquarters", "FAC-HQ", com.enterprise.spendsync.core.internal.domain.FacilityType.OFFICE, "Maslak Istanbul");
        facility.setId(UUID.randomUUID());

        vendor = new Vendor();
        vendor.setId(UUID.randomUUID());
        vendor.setName("Global Tech Supplies");
        vendor.setPaymentTerms(PaymentTerms.NET_30);

        creator = new User("procurement@spendsync.com", "pass", "Procurement", "Officer", null, "TR");
        creator.setId(UUID.randomUUID());
    }

    @ParameterizedTest(name = "Year {0}, Seq {1} -> PO Number {2}")
    @CsvSource({
            "2026, 1, PO-2026-00001",
            "2026, 42, PO-2026-00042",
            "2027, 99999, PO-2027-99999"
    })
    @DisplayName("Should generate standard enterprise PO numbers formatted as PO-YYYY-XXXXX")
    void shouldGenerateFormattedPoNumbers(int year, long seq, String expectedPoNumber) {
        String generated = PurchaseOrder.generatePoNumber(year, seq);
        assertThat(generated).isEqualTo(expectedPoNumber);
    }

    @Test
    @DisplayName("Should initialize Purchase Order in DRAFT status with zero amount")
    void shouldInitializePurchaseOrderDefaults() {
        PurchaseOrder po = new PurchaseOrder(
                tenant,
                "PO-2026-00001",
                null,
                legalEntity,
                costCenter,
                facility,
                vendor,
                Incoterms.DAP,
                "USD",
                PaymentTerms.NET_60,
                "Urgent delivery requested",
                creator
        );

        assertThat(po.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(po.getRevisionNumber()).isEqualTo(0);
        assertThat(po.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(po.getCurrency()).isEqualTo("USD");
        assertThat(po.getPaymentTerms()).isEqualTo(PaymentTerms.NET_60);
        assertThat(po.getIncoterms()).isEqualTo(Incoterms.DAP);
    }

    @Test
    @DisplayName("Should recalculate total amount when adding multiple line items")
    void shouldRecalculateTotalAmountOnAddingLineItems() {
        PurchaseOrder po = new PurchaseOrder(
                tenant, "PO-2026-00001", null, legalEntity, costCenter, facility, vendor,
                Incoterms.DAP, "TRY", PaymentTerms.NET_30, null, creator
        );

        PurchaseOrderLineItem item1 = new PurchaseOrderLineItem(
                tenant, po, null, 1, "Server Rack 42U", "IT_HARDWARE",
                new BigDecimal("2.0000"), "PIECE", new BigDecimal("15000.0000"),
                BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now().plusDays(15)
        );
        po.addLineItem(item1);

        assertThat(po.getTotalAmount()).isEqualByComparingTo(new BigDecimal("30000.0000"));

        PurchaseOrderLineItem item2 = new PurchaseOrderLineItem(
                tenant, po, null, 2, "PDU 16A", "IT_HARDWARE",
                new BigDecimal("4.0000"), "PIECE", new BigDecimal("2500.0000"),
                BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now().plusDays(15)
        );
        po.addLineItem(item2);

        assertThat(po.getTotalAmount()).isEqualByComparingTo(new BigDecimal("40000.0000"));
        assertThat(po.getLineItems()).hasSize(2);
    }
}
