package com.enterprise.spendsync.matching.internal.domain;

import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
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
        name = "invoice_discrepancies",
        indexes = {
                @Index(name = "idx_discrepancy_invoice", columnList = "supplier_invoice_id"),
                @Index(name = "idx_discrepancy_tenant", columnList = "tenant_id")
        }
)
public class InvoiceDiscrepancy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_invoice_id", nullable = false)
    private SupplierInvoice supplierInvoice;

    @Column(name = "discrepancy_type", nullable = false, length = 50)
    private String discrepancyType;

    @Column(name = "expected_value", length = 100)
    private String expectedValue;

    @Column(name = "actual_value", length = 100)
    private String actualValue;

    @Column(name = "variance_amount", precision = 18, scale = 4)
    private BigDecimal varianceAmount;

    @Column(name = "variance_percentage", precision = 6, scale = 2)
    private BigDecimal variancePercentage;

    @Column(name = "resolved", nullable = false)
    private boolean resolved = false;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_id")
    private User resolvedByUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected InvoiceDiscrepancy() {}

    public InvoiceDiscrepancy(Tenant tenant,
                              SupplierInvoice supplierInvoice,
                              String discrepancyType,
                              String expectedValue,
                              String actualValue,
                              BigDecimal varianceAmount,
                              BigDecimal variancePercentage) {
        this.tenant = tenant;
        this.supplierInvoice = supplierInvoice;
        this.discrepancyType = discrepancyType;
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
        this.varianceAmount = varianceAmount;
        this.variancePercentage = variancePercentage;
        this.resolved = false;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public void resolve(String resolutionNotes, User user) {
        this.resolved = true;
        this.resolutionNotes = resolutionNotes;
        this.resolvedByUser = user;
    }

    public UUID getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public SupplierInvoice getSupplierInvoice() { return supplierInvoice; }
    public String getDiscrepancyType() { return discrepancyType; }
    public String getExpectedValue() { return expectedValue; }
    public String getActualValue() { return actualValue; }
    public BigDecimal getVarianceAmount() { return varianceAmount; }
    public BigDecimal getVariancePercentage() { return variancePercentage; }
    public boolean isResolved() { return resolved; }
    public String getResolutionNotes() { return resolutionNotes; }
    public User getResolvedByUser() { return resolvedByUser; }
    public Instant getCreatedAt() { return createdAt; }
}
