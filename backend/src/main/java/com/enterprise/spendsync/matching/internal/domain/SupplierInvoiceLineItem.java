package com.enterprise.spendsync.matching.internal.domain;

import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderLineItem;
import com.enterprise.spendsync.receiving.internal.domain.GoodsReceiptLineItem;
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
import java.util.UUID;

@Entity
@Table(
        name = "supplier_invoice_line_items",
        indexes = {
                @Index(name = "idx_inv_line_items_inv", columnList = "supplier_invoice_id"),
                @Index(name = "idx_inv_line_items_po_line", columnList = "purchase_order_line_item_id")
        }
)
public class SupplierInvoiceLineItem {

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
    @JoinColumn(name = "purchase_order_line_item_id", nullable = false)
    private PurchaseOrderLineItem purchaseOrderLineItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_line_item_id")
    private GoodsReceiptLineItem goodsReceiptLineItem;

    @Column(name = "invoiced_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal invoicedQuantity;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "tax_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal taxAmount;

    @Column(name = "tevkifat_code", length = 20)
    private String tevkifatCode;

    @Column(name = "tevkifat_rate", length = 20)
    private String tevkifatRate;

    @Column(name = "tevkifat_amount", precision = 18, scale = 4)
    private BigDecimal tevkifatAmount = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 18, scale = 4)
    private BigDecimal lineTotalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 50)
    private InvoiceMatchStatus matchStatus = InvoiceMatchStatus.AUTO_MATCHED;

    @Column(name = "variance_reason", columnDefinition = "TEXT")
    private String varianceReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SupplierInvoiceLineItem() {
    }

    public SupplierInvoiceLineItem(Tenant tenant,
                                  PurchaseOrderLineItem purchaseOrderLineItem,
                                  GoodsReceiptLineItem goodsReceiptLineItem,
                                  BigDecimal invoicedQuantity,
                                  BigDecimal unitPrice,
                                  BigDecimal taxRate,
                                  BigDecimal taxAmount,
                                  BigDecimal lineTotalAmount) {
        this(tenant, purchaseOrderLineItem, goodsReceiptLineItem, invoicedQuantity, unitPrice, taxRate, taxAmount, null, null, BigDecimal.ZERO, lineTotalAmount);
    }

    public SupplierInvoiceLineItem(Tenant tenant,
                                  PurchaseOrderLineItem purchaseOrderLineItem,
                                  GoodsReceiptLineItem goodsReceiptLineItem,
                                  BigDecimal invoicedQuantity,
                                  BigDecimal unitPrice,
                                  BigDecimal taxRate,
                                  BigDecimal taxAmount,
                                  String tevkifatCode,
                                  String tevkifatRate,
                                  BigDecimal tevkifatAmount,
                                  BigDecimal lineTotalAmount) {
        this.tenant = tenant;
        this.purchaseOrderLineItem = purchaseOrderLineItem;
        this.goodsReceiptLineItem = goodsReceiptLineItem;
        this.invoicedQuantity = invoicedQuantity;
        this.unitPrice = unitPrice;
        this.taxRate = taxRate;
        this.taxAmount = taxAmount;
        this.tevkifatCode = tevkifatCode;
        this.tevkifatRate = tevkifatRate;
        this.tevkifatAmount = tevkifatAmount != null ? tevkifatAmount : BigDecimal.ZERO;
        this.lineTotalAmount = lineTotalAmount;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Tenant getTenant() { return tenant; }
    public SupplierInvoice getSupplierInvoice() { return supplierInvoice; }
    public void setSupplierInvoice(SupplierInvoice supplierInvoice) { this.supplierInvoice = supplierInvoice; }
    public PurchaseOrderLineItem getPurchaseOrderLineItem() { return purchaseOrderLineItem; }
    public GoodsReceiptLineItem getGoodsReceiptLineItem() { return goodsReceiptLineItem; }
    public void setGoodsReceiptLineItem(GoodsReceiptLineItem goodsReceiptLineItem) { this.goodsReceiptLineItem = goodsReceiptLineItem; }
    public BigDecimal getInvoicedQuantity() { return invoicedQuantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTaxRate() { return taxRate; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public String getTevkifatCode() { return tevkifatCode; }
    public String getTevkifatRate() { return tevkifatRate; }
    public BigDecimal getTevkifatAmount() { return tevkifatAmount; }
    public BigDecimal getLineTotalAmount() { return lineTotalAmount; }
    public BigDecimal getLineTotal() { return lineTotalAmount; }
    public InvoiceMatchStatus getMatchStatus() { return matchStatus; }
    public void setMatchStatus(InvoiceMatchStatus matchStatus) { this.matchStatus = matchStatus; }
    public String getVarianceReason() { return varianceReason; }
    public void setVarianceReason(String varianceReason) { this.varianceReason = varianceReason; }
    public Instant getCreatedAt() { return createdAt; }
}
