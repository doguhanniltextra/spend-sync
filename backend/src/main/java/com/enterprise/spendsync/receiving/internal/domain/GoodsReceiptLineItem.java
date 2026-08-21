package com.enterprise.spendsync.receiving.internal.domain;

import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderLineItem;
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
        name = "goods_receipt_line_items",
        indexes = {
                @Index(name = "idx_gr_line_items_gr", columnList = "goods_receipt_id"),
                @Index(name = "idx_gr_line_items_po_line", columnList = "purchase_order_line_item_id")
        }
)
public class GoodsReceiptLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private GoodsReceipt goodsReceipt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_line_item_id", nullable = false)
    private PurchaseOrderLineItem purchaseOrderLineItem;

    @Column(name = "received_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal receivedQuantity;

    @Column(name = "accepted_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal acceptedQuantity;

    @Column(name = "rejected_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal rejectedQuantity = BigDecimal.ZERO;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected GoodsReceiptLineItem() {
    }

    public GoodsReceiptLineItem(Tenant tenant,
                                PurchaseOrderLineItem purchaseOrderLineItem,
                                BigDecimal receivedQuantity,
                                BigDecimal acceptedQuantity,
                                BigDecimal rejectedQuantity,
                                String rejectionReason,
                                String notes) {
        this.tenant = tenant;
        this.purchaseOrderLineItem = purchaseOrderLineItem;
        this.receivedQuantity = receivedQuantity;
        this.acceptedQuantity = acceptedQuantity;
        this.rejectedQuantity = rejectedQuantity != null ? rejectedQuantity : BigDecimal.ZERO;
        this.rejectionReason = rejectionReason;
        this.notes = notes;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Getters & Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Tenant getTenant() { return tenant; }
    public GoodsReceipt getGoodsReceipt() { return goodsReceipt; }
    public void setGoodsReceipt(GoodsReceipt goodsReceipt) { this.goodsReceipt = goodsReceipt; }
    public PurchaseOrderLineItem getPurchaseOrderLineItem() { return purchaseOrderLineItem; }
    public BigDecimal getReceivedQuantity() { return receivedQuantity; }
    public BigDecimal getAcceptedQuantity() { return acceptedQuantity; }
    public BigDecimal getRejectedQuantity() { return rejectedQuantity; }
    public String getRejectionReason() { return rejectionReason; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
}
