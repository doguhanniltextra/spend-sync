package com.enterprise.spendsync.purchasing.internal.domain;

import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.Facility;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.requisition.internal.domain.PurchaseRequisition;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Purchase Order (PO) Entity.
 * Represents a legally binding external commercial contract between the company and a Vendor.
 */
@Entity
@Table(name = "purchase_orders", uniqueConstraints = {
        @UniqueConstraint(name = "uk_po_number_tenant", columnNames = {"tenant_id", "po_number"})
})
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "po_number", nullable = false, length = 50)
    private String poNumber;

    @Column(name = "revision_number", nullable = false)
    private int revisionNumber = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisition_id")
    private PurchaseRequisition requisition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    private LegalEntity legalEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cost_center_id", nullable = false)
    private CostCenter costCenter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_facility_id", nullable = false)
    private Facility deliveryFacility;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "incoterms", nullable = false, length = 50)
    private Incoterms incoterms = Incoterms.DAP;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "TRY";

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_terms", nullable = false, length = 50)
    private PaymentTerms paymentTerms = PaymentTerms.NET_30;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    private List<PurchaseOrderLineItem> lineItems = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PurchaseOrder() {
    }

    public PurchaseOrder(Tenant tenant,
                         String poNumber,
                         PurchaseRequisition requisition,
                         LegalEntity legalEntity,
                         CostCenter costCenter,
                         Facility deliveryFacility,
                         Vendor vendor,
                         Incoterms incoterms,
                         String currency,
                         PaymentTerms paymentTerms,
                         String notes,
                         User createdByUser) {
        this.tenant = tenant;
        this.poNumber = poNumber;
        this.requisition = requisition;
        this.legalEntity = legalEntity;
        this.costCenter = costCenter;
        this.deliveryFacility = deliveryFacility;
        this.vendor = vendor;
        this.incoterms = incoterms != null ? incoterms : Incoterms.DAP;
        this.currency = currency != null ? currency.toUpperCase() : "TRY";
        this.paymentTerms = paymentTerms != null ? paymentTerms : (vendor != null ? vendor.getPaymentTerms() : PaymentTerms.NET_30);
        this.notes = notes;
        this.createdByUser = createdByUser;
        this.status = PurchaseOrderStatus.DRAFT;
        this.revisionNumber = 0;
        this.totalAmount = BigDecimal.ZERO;
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

    public void addLineItem(PurchaseOrderLineItem item) {
        this.lineItems.add(item);
        item.setPurchaseOrder(this);
        recalculateTotal();
    }

    public void recalculateTotal() {
        this.totalAmount = this.lineItems.stream()
                .map(PurchaseOrderLineItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static String generatePoNumber(int fiscalYear, long sequenceNumber) {
        return String.format("PO-%d-%05d", fiscalYear, sequenceNumber);
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Tenant getTenant() { return tenant; }
    public String getPoNumber() { return poNumber; }
    public int getRevisionNumber() { return revisionNumber; }
    public void setRevisionNumber(int revisionNumber) { this.revisionNumber = revisionNumber; }
    public PurchaseRequisition getRequisition() { return requisition; }
    public void setRequisition(PurchaseRequisition requisition) { this.requisition = requisition; }
    public LegalEntity getLegalEntity() { return legalEntity; }
    public CostCenter getCostCenter() { return costCenter; }
    public Facility getDeliveryFacility() { return deliveryFacility; }
    public void setDeliveryFacility(Facility deliveryFacility) { this.deliveryFacility = deliveryFacility; }
    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }
    public PurchaseOrderStatus getStatus() { return status; }
    public void setStatus(PurchaseOrderStatus status) { this.status = status; }
    public Incoterms getIncoterms() { return incoterms; }
    public void setIncoterms(Incoterms incoterms) { this.incoterms = incoterms; }
    public String getCurrency() { return currency; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public PaymentTerms getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(PaymentTerms paymentTerms) { this.paymentTerms = paymentTerms; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
    public User getCreatedByUser() { return createdByUser; }
    public List<PurchaseOrderLineItem> getLineItems() { return lineItems; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
