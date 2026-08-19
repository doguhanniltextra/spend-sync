package com.enterprise.spendsync.vendorportal.internal.domain;

import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoice;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "vendor_early_pay_offers",
        indexes = {
                @Index(name = "idx_early_pay_invoice", columnList = "supplier_invoice_id"),
                @Index(name = "idx_early_pay_vendor", columnList = "tenant_id, vendor_id")
        }
)
public class VendorEarlyPayOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_invoice_id", nullable = false)
    private SupplierInvoice supplierInvoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "original_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal originalAmount;

    @Column(name = "original_due_date", nullable = false)
    private LocalDate originalDueDate;

    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal discountAmount;

    @Column(name = "net_payout_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal netPayoutAmount;

    @Column(name = "accelerated_payment_date", nullable = false)
    private LocalDate acceleratedPaymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private EarlyPayOfferStatus status = EarlyPayOfferStatus.OFFERED;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected VendorEarlyPayOffer() {}

    public VendorEarlyPayOffer(Tenant tenant,
                               SupplierInvoice supplierInvoice,
                               Vendor vendor,
                               BigDecimal originalAmount,
                               LocalDate originalDueDate,
                               BigDecimal discountPercentage,
                               BigDecimal discountAmount,
                               BigDecimal netPayoutAmount,
                               LocalDate acceleratedPaymentDate) {
        this.tenant = tenant;
        this.supplierInvoice = supplierInvoice;
        this.vendor = vendor;
        this.originalAmount = originalAmount;
        this.originalDueDate = originalDueDate;
        this.discountPercentage = discountPercentage;
        this.discountAmount = discountAmount;
        this.netPayoutAmount = netPayoutAmount;
        this.acceleratedPaymentDate = acceleratedPaymentDate;
        this.status = EarlyPayOfferStatus.OFFERED;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public void accept() {
        this.status = EarlyPayOfferStatus.ACCEPTED;
        this.acceptedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public SupplierInvoice getSupplierInvoice() { return supplierInvoice; }
    public Vendor getVendor() { return vendor; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public LocalDate getOriginalDueDate() { return originalDueDate; }
    public BigDecimal getDiscountPercentage() { return discountPercentage; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getNetPayoutAmount() { return netPayoutAmount; }
    public LocalDate getAcceleratedPaymentDate() { return acceleratedPaymentDate; }
    public EarlyPayOfferStatus getStatus() { return status; }
    public void setStatus(EarlyPayOfferStatus status) { this.status = status; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
