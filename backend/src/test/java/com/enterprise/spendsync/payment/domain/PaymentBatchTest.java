package com.enterprise.spendsync.payment.domain;

import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.matching.internal.domain.InvoiceProfile;
import com.enterprise.spendsync.matching.internal.domain.InvoiceType;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoice;
import com.enterprise.spendsync.payment.internal.domain.PaymentBatch;
import com.enterprise.spendsync.payment.internal.domain.PaymentBatchItem;
import com.enterprise.spendsync.payment.internal.domain.PaymentBatchStatus;
import com.enterprise.spendsync.payment.internal.domain.PaymentMethod;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentBatch Domain Entity Pure Unit Tests")
class PaymentBatchTest {

    private Tenant tenant;
    private LegalEntity legalEntity;
    private User creator;
    private Vendor vendor;
    private SupplierInvoice invoice;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName("SpendSync Global");

        legalEntity = new LegalEntity(tenant, "SpendSync Turkey", "TR01", "1234567890", "TRY", "Istanbul", "TR");
        legalEntity.setId(UUID.randomUUID());

        creator = new User("ap@spendsync.com", "pass", "AP", "Officer", null, "TR");
        creator.setId(UUID.randomUUID());

        vendor = new Vendor();
        vendor.setId(UUID.randomUUID());
        vendor.setName("Global Server Supplies");

        invoice = new SupplierInvoice(
                tenant, "INV-2026-0001", "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                LocalDate.now(), InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA,
                null, vendor, legalEntity, null, "TRY",
                new BigDecimal("100000.00"), new BigDecimal("20000.00"), new BigDecimal("120000.00")
        );
        invoice.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should initialize PaymentBatch defaults and track attached line items")
    void shouldInitializePaymentBatchDefaults() {
        PaymentBatch batch = new PaymentBatch(
                tenant,
                "PAY-2026-00001",
                legalEntity,
                PaymentMethod.ISO_20022_PAIN_001,
                new BigDecimal("120000.00"),
                "TRY",
                creator,
                "idempotency-key-001"
        );

        PaymentBatchItem item = new PaymentBatchItem(
                tenant,
                invoice,
                vendor,
                "Global Server Supplies",
                "TR330006200000012345678901",
                new BigDecimal("120000.00"),
                BigDecimal.ZERO,
                new BigDecimal("120000.00")
        );

        batch.addLineItem(item);

        assertThat(batch.getBatchNumber()).isEqualTo("PAY-2026-00001");
        assertThat(batch.getStatus()).isEqualTo(PaymentBatchStatus.DRAFT);
        assertThat(batch.getPaymentMethod()).isEqualTo(PaymentMethod.ISO_20022_PAIN_001);
        assertThat(batch.getItemCount()).isEqualTo(1);
        assertThat(batch.getLineItems()).hasSize(1);
        assertThat(batch.getLineItems().get(0).getPaymentBatch()).isEqualTo(batch);
        assertThat(batch.getLineItems().get(0).getStatus()).isEqualTo("INCLUDED");
    }
}
