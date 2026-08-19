package com.enterprise.spendsync.vendorportal.internal.domain;

import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrder;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "vendor_po_acknowledgments",
        indexes = {
                @Index(name = "idx_po_ack_po_id", columnList = "purchase_order_id"),
                @Index(name = "idx_po_ack_tenant_vendor", columnList = "tenant_id, vendor_id")
        }
)
public class VendorPoAcknowledgment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acknowledged_by_user_id", nullable = false)
    private VendorUser acknowledgedByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private VendorPoAcknowledgmentStatus status;

    @Column(name = "promised_delivery_date")
    private LocalDate promisedDeliveryDate;

    @Column(name = "vendor_notes", columnDefinition = "TEXT")
    private String vendorNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected VendorPoAcknowledgment() {}

    public VendorPoAcknowledgment(Tenant tenant,
                                  PurchaseOrder purchaseOrder,
                                  Vendor vendor,
                                  VendorUser acknowledgedByUser,
                                  VendorPoAcknowledgmentStatus status,
                                  LocalDate promisedDeliveryDate,
                                  String vendorNotes) {
        this.tenant = tenant;
        this.purchaseOrder = purchaseOrder;
        this.vendor = vendor;
        this.acknowledgedByUser = acknowledgedByUser;
        this.status = status;
        this.promisedDeliveryDate = promisedDeliveryDate;
        this.vendorNotes = vendorNotes;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public Vendor getVendor() { return vendor; }
    public VendorUser getAcknowledgedByUser() { return acknowledgedByUser; }
    public VendorPoAcknowledgmentStatus getStatus() { return status; }
    public LocalDate getPromisedDeliveryDate() { return promisedDeliveryDate; }
    public String getVendorNotes() { return vendorNotes; }
    public Instant getCreatedAt() { return createdAt; }
}
