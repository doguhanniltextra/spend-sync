package com.enterprise.spendsync.receiving.internal.domain;

import com.enterprise.spendsync.core.internal.domain.Facility;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrder;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "goods_receipts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_goods_receipt_number", columnNames = {"tenant_id", "receipt_number"})
        },
        indexes = {
                @Index(name = "idx_goods_receipts_po", columnList = "purchase_order_id"),
                @Index(name = "idx_goods_receipts_tenant_created", columnList = "tenant_id, created_at DESC")
        }
)
public class GoodsReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "receipt_number", nullable = false, length = 50)
    private String receiptNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_facility_id", nullable = false)
    private Facility deliveryFacility;

    @Column(name = "waybill_number", nullable = false, length = 100)
    private String waybillNumber;

    @Column(name = "waybill_date", nullable = false)
    private LocalDate waybillDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "received_by_user_id", nullable = false)
    private User receivedByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private GoodsReceiptStatus status = GoodsReceiptStatus.COMPLETED;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoodsReceiptLineItem> lineItems = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected GoodsReceipt() {
    }

    public GoodsReceipt(Tenant tenant,
                        String receiptNumber,
                        PurchaseOrder purchaseOrder,
                        Facility deliveryFacility,
                        String waybillNumber,
                        LocalDate waybillDate,
                        User receivedByUser,
                        String notes) {
        this.tenant = tenant;
        this.receiptNumber = receiptNumber;
        this.purchaseOrder = purchaseOrder;
        this.deliveryFacility = deliveryFacility;
        this.waybillNumber = waybillNumber;
        this.waybillDate = waybillDate;
        this.receivedByUser = receivedByUser;
        this.notes = notes;
        this.status = GoodsReceiptStatus.COMPLETED;
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

    public void addLineItem(GoodsReceiptLineItem item) {
        lineItems.add(item);
        item.setGoodsReceipt(this);
    }

    // Getters & Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Tenant getTenant() { return tenant; }
    public String getReceiptNumber() { return receiptNumber; }
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public Facility getDeliveryFacility() { return deliveryFacility; }
    public String getWaybillNumber() { return waybillNumber; }
    public LocalDate getWaybillDate() { return waybillDate; }
    public User getReceivedByUser() { return receivedByUser; }
    public GoodsReceiptStatus getStatus() { return status; }
    public void setStatus(GoodsReceiptStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<GoodsReceiptLineItem> getLineItems() { return lineItems; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
