package com.enterprise.spendsync.receiving.domain;

import com.enterprise.spendsync.core.internal.domain.Facility;
import com.enterprise.spendsync.core.internal.domain.FacilityType;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.purchasing.internal.domain.Incoterms;
import com.enterprise.spendsync.purchasing.internal.domain.PaymentTerms;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrder;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderLineItem;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import com.enterprise.spendsync.receiving.internal.domain.GoodsReceipt;
import com.enterprise.spendsync.receiving.internal.domain.GoodsReceiptLineItem;
import com.enterprise.spendsync.receiving.internal.domain.GoodsReceiptStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GoodsReceipt Domain Entity Pure Unit Tests")
class GoodsReceiptTest {

    private Tenant tenant;
    private LegalEntity legalEntity;
    private Facility facility;
    private User receiver;
    private PurchaseOrder po;
    private PurchaseOrderLineItem poLine;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName("SpendSync Global");

        legalEntity = new LegalEntity(tenant, "SpendSync Turkey", "TR01", "1234567890", "TRY", "Istanbul", "TR");
        legalEntity.setId(UUID.randomUUID());

        facility = new Facility(tenant, legalEntity, "Main Warehouse", "FAC-01", FacilityType.WAREHOUSE, "Gebze OSB");
        facility.setId(UUID.randomUUID());

        receiver = new User("warehouse@spendsync.com", "pass", "Warehouse", "Officer", null, "TR");
        receiver.setId(UUID.randomUUID());

        Vendor vendor = new Vendor();
        vendor.setId(UUID.randomUUID());
        vendor.setName("Global Hardware Inc.");

        po = new PurchaseOrder(
                tenant, "PO-2026-00001", null, legalEntity, null, facility,
                vendor, Incoterms.DAP, "TRY", PaymentTerms.NET_30, null, receiver
        );
        po.setId(UUID.randomUUID());

        poLine = new PurchaseOrderLineItem(
                tenant, po, null, 1, "Server Rack 42U", "IT_HARDWARE",
                new BigDecimal("10.0000"), "PIECE", new BigDecimal("15000.0000"),
                new BigDecimal("5.00"), BigDecimal.ZERO, LocalDate.now().plusDays(10)
        );
        poLine.setId(UUID.randomUUID());
        po.addLineItem(poLine);
    }

    @Test
    @DisplayName("Should initialize Goods Receipt in COMPLETED status with lines attached")
    void shouldInitializeGoodsReceiptDefaults() {
        GoodsReceipt gr = new GoodsReceipt(
                tenant,
                "GR-2026-00001",
                po,
                facility,
                "IRS-2026-99988",
                LocalDate.now(),
                receiver,
                "Delivered by Aras Kargo"
        );

        GoodsReceiptLineItem grLine = new GoodsReceiptLineItem(
                tenant,
                poLine,
                new BigDecimal("10.0000"),
                new BigDecimal("8.0000"),
                new BigDecimal("2.0000"),
                "2 units damaged in transit",
                "Returned with driver"
        );

        gr.addLineItem(grLine);

        assertThat(gr.getStatus()).isEqualTo(GoodsReceiptStatus.COMPLETED);
        assertThat(gr.getReceiptNumber()).isEqualTo("GR-2026-00001");
        assertThat(gr.getWaybillNumber()).isEqualTo("IRS-2026-99988");
        assertThat(gr.getLineItems()).hasSize(1);
        assertThat(gr.getLineItems().get(0).getGoodsReceipt()).isEqualTo(gr);
        assertThat(gr.getLineItems().get(0).getAcceptedQuantity()).isEqualByComparingTo(new BigDecimal("8.0000"));
        assertThat(gr.getLineItems().get(0).getRejectedQuantity()).isEqualByComparingTo(new BigDecimal("2.0000"));
        assertThat(gr.getLineItems().get(0).getRejectionReason()).isEqualTo("2 units damaged in transit");
    }
}
