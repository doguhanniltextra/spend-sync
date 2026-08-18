package com.enterprise.spendsync.matching.internal.domain;

import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrder;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "supplier_invoices",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_supplier_invoice_ettn", columnNames = {"tenant_id", "ettn"}),
                @UniqueConstraint(name = "uk_supplier_invoice_vendor_no", columnNames = {"tenant_id", "vendor_id", "invoice_number"})
        },
        indexes = {
                @Index(name = "idx_supplier_invoices_po", columnList = "purchase_order_id"),
                @Index(name = "idx_supplier_invoices_tenant_created", columnList = "tenant_id, created_at DESC")
        }
)
public class SupplierInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "invoice_number", nullable = false, length = 50)
    private String invoiceNumber;

    @Column(name = "ettn", nullable = false, length = 100)
    private String ettn;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 50)
    private InvoiceType invoiceType = InvoiceType.SATIS;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_profile", nullable = false, length = 50)
    private InvoiceProfile invoiceProfile = InvoiceProfile.TICARI_FATURA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    private LegalEntity legalEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cost_center_id", nullable = false)
    private CostCenter costCenter;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "subtotal_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal subtotalAmount;

    @Column(name = "tax_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 50)
    private InvoiceMatchStatus matchStatus = InvoiceMatchStatus.EVALUATING;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private InvoiceStatus status = InvoiceStatus.SUBMITTED;

    @Column(name = "discrepancy_reason", columnDefinition = "TEXT")
    private String discrepancyReason;

    @Column(name = "manager_override_note", columnDefinition = "TEXT")
    private String managerOverrideNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_override_by_user_id")
    private User managerOverrideByUser;

    @OneToMany(mappedBy = "supplierInvoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupplierInvoiceLineItem> lineItems = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SupplierInvoice() {
    }

    public SupplierInvoice(Tenant tenant,
                           String invoiceNumber,
                           String ettn,
                           LocalDate invoiceDate,
                           InvoiceType invoiceType,
                           InvoiceProfile invoiceProfile,
                           PurchaseOrder purchaseOrder,
                           Vendor vendor,
                           LegalEntity legalEntity,
                           CostCenter costCenter,
                           String currency,
                           BigDecimal subtotalAmount,
                           BigDecimal taxAmount,
                           BigDecimal totalAmount) {
        this.tenant = tenant;
        this.invoiceNumber = invoiceNumber;
        this.ettn = ettn;
        this.invoiceDate = invoiceDate;
        this.invoiceType = invoiceType != null ? invoiceType : InvoiceType.SATIS;
        this.invoiceProfile = invoiceProfile != null ? invoiceProfile : InvoiceProfile.TICARI_FATURA;
        this.purchaseOrder = purchaseOrder;
        this.vendor = vendor;
        this.legalEntity = legalEntity;
        this.costCenter = costCenter;
        this.currency = currency;
        this.subtotalAmount = subtotalAmount;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.matchStatus = InvoiceMatchStatus.EVALUATING;
        this.status = InvoiceStatus.SUBMITTED;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addLineItem(SupplierInvoiceLineItem item) {
        lineItems.add(item);
        item.setSupplierInvoice(this);
    }

    // Getters & Setters
    public UUID getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public String getEttn() { return ettn; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public InvoiceType getInvoiceType() { return invoiceType; }
    public InvoiceProfile getInvoiceProfile() { return invoiceProfile; }
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public Vendor getVendor() { return vendor; }
    public LegalEntity getLegalEntity() { return legalEntity; }
    public CostCenter getCostCenter() { return costCenter; }
    public String getCurrency() { return currency; }
    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public InvoiceMatchStatus getMatchStatus() { return matchStatus; }
    public void setMatchStatus(InvoiceMatchStatus matchStatus) { this.matchStatus = matchStatus; }
    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }
    public String getDiscrepancyReason() { return discrepancyReason; }
    public void setDiscrepancyReason(String discrepancyReason) { this.discrepancyReason = discrepancyReason; }
    public String getManagerOverrideNote() { return managerOverrideNote; }
    public void setManagerOverrideNote(String managerOverrideNote) { this.managerOverrideNote = managerOverrideNote; }
    public User getManagerOverrideByUser() { return managerOverrideByUser; }
    public void setManagerOverrideByUser(User managerOverrideByUser) { this.managerOverrideByUser = managerOverrideByUser; }
    public List<SupplierInvoiceLineItem> getLineItems() { return lineItems; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
