package com.enterprise.spendsync.vendorportal.internal.domain;

import com.enterprise.spendsync.core.internal.domain.Tenant;
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
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "vendor_monthly_reconciliations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_reconciliation_vendor_period", columnNames = {"tenant_id", "vendor_id", "period_year", "period_month"})
        },
        indexes = {
                @Index(name = "idx_reconciliation_vendor", columnList = "tenant_id, vendor_id")
        }
)
public class VendorMonthlyReconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "period_year", nullable = false)
    private int periodYear;

    @Column(name = "period_month", nullable = false)
    private int periodMonth;

    @Column(name = "invoice_count", nullable = false)
    private int invoiceCount;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING"; // PENDING, APPROVED, DISPUTED

    @Column(name = "vendor_approved_at")
    private Instant vendorApprovedAt;

    @Column(name = "vendor_notes", columnDefinition = "TEXT")
    private String vendorNotes;

    @Column(name = "signed_checksum", length = 64)
    private String signedChecksum;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected VendorMonthlyReconciliation() {}

    public VendorMonthlyReconciliation(Tenant tenant,
                                       Vendor vendor,
                                       int periodYear,
                                       int periodMonth,
                                       int invoiceCount,
                                       BigDecimal totalAmount) {
        this.tenant = tenant;
        this.vendor = vendor;
        this.periodYear = periodYear;
        this.periodMonth = periodMonth;
        this.invoiceCount = invoiceCount;
        this.totalAmount = totalAmount;
        this.status = "PENDING";
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public void approve(String notes, String checksum) {
        this.status = "APPROVED";
        this.vendorNotes = notes;
        this.signedChecksum = checksum;
        this.vendorApprovedAt = Instant.now();
    }

    public void dispute(String notes) {
        this.status = "DISPUTED";
        this.vendorNotes = notes;
        this.vendorApprovedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public Vendor getVendor() { return vendor; }
    public int getPeriodYear() { return periodYear; }
    public int getPeriodMonth() { return periodMonth; }
    public int getInvoiceCount() { return invoiceCount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public Instant getVendorApprovedAt() { return vendorApprovedAt; }
    public String getVendorNotes() { return vendorNotes; }
    public String getSignedChecksum() { return signedChecksum; }
    public Instant getCreatedAt() { return createdAt; }
}
