package com.enterprise.spendsync.purchasing.internal.domain;

import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionLineItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Purchase Order Line Item Entity.
 */
@Entity
@Table(name = "purchase_order_line_items")
public class PurchaseOrderLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisition_line_item_id")
    private RequisitionLineItem requisitionLineItem;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(name = "item_description", nullable = false, length = 255)
    private String itemDescription;

    @Column(name = "item_category", nullable = false, length = 100)
    private String itemCategory;

    @Column(name = "quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_of_measure", nullable = false, length = 50)
    private String unitOfMeasure = "PIECE";

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalPrice;

    @Column(name = "over_delivery_tolerance_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal overDeliveryTolerancePct = BigDecimal.ZERO;

    @Column(name = "under_delivery_tolerance_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal underDeliveryTolerancePct = BigDecimal.ZERO;

    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PurchaseOrderLineItem() {
    }

    public PurchaseOrderLineItem(Tenant tenant,
                                 PurchaseOrder purchaseOrder,
                                 RequisitionLineItem requisitionLineItem,
                                 int lineNumber,
                                 String itemDescription,
                                 String itemCategory,
                                 BigDecimal quantity,
                                 String unitOfMeasure,
                                 BigDecimal unitPrice,
                                 BigDecimal overDeliveryTolerancePct,
                                 BigDecimal underDeliveryTolerancePct,
                                 LocalDate estimatedDeliveryDate) {
        this.tenant = tenant;
        this.purchaseOrder = purchaseOrder;
        this.requisitionLineItem = requisitionLineItem;
        this.lineNumber = lineNumber;
        this.itemDescription = itemDescription;
        this.itemCategory = itemCategory != null ? itemCategory : "GENERAL";
        this.quantity = quantity;
        this.unitOfMeasure = unitOfMeasure != null ? unitOfMeasure : "PIECE";
        this.unitPrice = unitPrice;
        this.totalPrice = quantity.multiply(unitPrice);
        this.overDeliveryTolerancePct = overDeliveryTolerancePct != null ? overDeliveryTolerancePct : BigDecimal.ZERO;
        this.underDeliveryTolerancePct = underDeliveryTolerancePct != null ? underDeliveryTolerancePct : BigDecimal.ZERO;
        this.estimatedDeliveryDate = estimatedDeliveryDate;
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

    public void updatePriceAndQuantity(BigDecimal newQuantity, BigDecimal newUnitPrice) {
        this.quantity = newQuantity;
        this.unitPrice = newUnitPrice;
        this.totalPrice = newQuantity.multiply(newUnitPrice);
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Tenant getTenant() { return tenant; }
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }
    public RequisitionLineItem getRequisitionLineItem() { return requisitionLineItem; }
    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    public String getItemDescription() { return itemDescription; }
    public String getItemCategory() { return itemCategory; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public BigDecimal getOverDeliveryTolerancePct() { return overDeliveryTolerancePct; }
    public void setOverDeliveryTolerancePct(BigDecimal overDeliveryTolerancePct) { this.overDeliveryTolerancePct = overDeliveryTolerancePct; }
    public BigDecimal getUnderDeliveryTolerancePct() { return underDeliveryTolerancePct; }
    public void setUnderDeliveryTolerancePct(BigDecimal underDeliveryTolerancePct) { this.underDeliveryTolerancePct = underDeliveryTolerancePct; }
    public LocalDate getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
    public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) { this.estimatedDeliveryDate = estimatedDeliveryDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
