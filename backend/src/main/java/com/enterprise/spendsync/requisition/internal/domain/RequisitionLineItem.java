package com.enterprise.spendsync.requisition.internal.domain;

import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "requisition_line_items",
        indexes = {
                @Index(name = "idx_pr_items_requisition", columnList = "requisition_id"),
                @Index(name = "idx_pr_items_tenant", columnList = "tenant_id")
        }
)
public class RequisitionLineItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requisition_id", nullable = false)
    private PurchaseRequisition requisition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(name = "item_description", nullable = false, length = 255)
    private String itemDescription;

    @Column(name = "item_category", nullable = false, length = 50)
    private String itemCategory;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure = "PIECE";

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalPrice;

    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    protected RequisitionLineItem() {
        super();
    }

    public RequisitionLineItem(PurchaseRequisition requisition,
                               Tenant tenant,
                               int lineNumber,
                               String itemDescription,
                               String itemCategory,
                               BigDecimal quantity,
                               String unitOfMeasure,
                               BigDecimal unitPrice,
                               LocalDate estimatedDeliveryDate) {
        super();
        this.requisition = requisition;
        this.tenant = tenant;
        this.lineNumber = lineNumber;
        this.itemDescription = itemDescription;
        this.itemCategory = itemCategory;
        this.quantity = quantity;
        this.unitOfMeasure = unitOfMeasure != null ? unitOfMeasure : "PIECE";
        this.unitPrice = unitPrice;
        this.totalPrice = (quantity != null && unitPrice != null) ? quantity.multiply(unitPrice) : BigDecimal.ZERO;
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public PurchaseRequisition getRequisition() {
        return requisition;
    }

    public void setRequisition(PurchaseRequisition requisition) {
        this.requisition = requisition;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public String getItemCategory() {
        return itemCategory;
    }

    public void setItemCategory(String itemCategory) {
        this.itemCategory = itemCategory;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        recalculateTotalPrice();
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        recalculateTotalPrice();
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public LocalDate getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    private void recalculateTotalPrice() {
        if (this.quantity != null && this.unitPrice != null) {
            this.totalPrice = this.quantity.multiply(this.unitPrice);
        }
    }
}
