package com.enterprise.spendsync.vendorportal.internal.domain;

import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderLineItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "vendor_asn_shipment_line_items")
public class VendorAsnShipmentLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asn_shipment_id", nullable = false)
    private VendorAsnShipment asnShipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_line_item_id", nullable = false)
    private PurchaseOrderLineItem purchaseOrderLineItem;

    @Column(name = "shipped_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal shippedQuantity;

    @Column(name = "unit_of_measure", nullable = false, length = 50)
    private String unitOfMeasure = "PIECE";

    @Column(name = "lot_number", length = 100)
    private String lotNumber;

    @Column(name = "serial_numbers", columnDefinition = "TEXT")
    private String serialNumbers;

    protected VendorAsnShipmentLineItem() {}

    public VendorAsnShipmentLineItem(PurchaseOrderLineItem purchaseOrderLineItem,
                                    BigDecimal shippedQuantity,
                                    String unitOfMeasure,
                                    String lotNumber,
                                    String serialNumbers) {
        this.purchaseOrderLineItem = purchaseOrderLineItem;
        this.shippedQuantity = shippedQuantity;
        this.unitOfMeasure = unitOfMeasure != null ? unitOfMeasure : "PIECE";
        this.lotNumber = lotNumber;
        this.serialNumbers = serialNumbers;
    }

    public UUID getId() { return id; }
    public VendorAsnShipment getAsnShipment() { return asnShipment; }
    public void setAsnShipment(VendorAsnShipment asnShipment) { this.asnShipment = asnShipment; }
    public PurchaseOrderLineItem getPurchaseOrderLineItem() { return purchaseOrderLineItem; }
    public BigDecimal getShippedQuantity() { return shippedQuantity; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public String getLotNumber() { return lotNumber; }
    public String getSerialNumbers() { return serialNumbers; }
}
