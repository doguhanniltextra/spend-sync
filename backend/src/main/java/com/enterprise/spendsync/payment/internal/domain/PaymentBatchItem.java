package com.enterprise.spendsync.payment.internal.domain;

import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoice;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "payment_batch_items",
        indexes = {
                @Index(name = "idx_pay_items_batch", columnList = "payment_batch_id"),
                @Index(name = "idx_pay_items_inv", columnList = "supplier_invoice_id")
        }
)
public class PaymentBatchItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_batch_id", nullable = false)
    private PaymentBatch paymentBatch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_invoice_id", nullable = false)
    private SupplierInvoice supplierInvoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "vendor_name", nullable = false)
    private String vendorName;

    @Column(name = "vendor_iban", nullable = false, length = 50)
    private String vendorIban;

    @Column(name = "amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "net_payable_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal netPayableAmount;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "INCLUDED";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentBatchItem() {
    }

    public PaymentBatchItem(Tenant tenant,
                            SupplierInvoice supplierInvoice,
                            Vendor vendor,
                            String vendorName,
                            String vendorIban,
                            BigDecimal amount,
                            BigDecimal discountAmount,
                            BigDecimal netPayableAmount) {
        this.tenant = tenant;
        this.supplierInvoice = supplierInvoice;
        this.vendor = vendor;
        this.vendorName = vendorName;
        this.vendorIban = vendorIban;
        this.amount = amount;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.netPayableAmount = netPayableAmount;
        this.status = "INCLUDED";
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Getters & Setters
    public UUID getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public PaymentBatch getPaymentBatch() { return paymentBatch; }
    public void setPaymentBatch(PaymentBatch paymentBatch) { this.paymentBatch = paymentBatch; }
    public SupplierInvoice getSupplierInvoice() { return supplierInvoice; }
    public Vendor getVendor() { return vendor; }
    public String getVendorName() { return vendorName; }
    public String getVendorIban() { return vendorIban; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getNetPayableAmount() { return netPayableAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}
