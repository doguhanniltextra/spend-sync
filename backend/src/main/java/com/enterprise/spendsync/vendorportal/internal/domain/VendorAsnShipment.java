package com.enterprise.spendsync.vendorportal.internal.domain;

import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrder;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import com.enterprise.spendsync.shared.crypto.EncryptedStringConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "vendor_asn_shipments",
        indexes = {
                @Index(name = "idx_asn_po_id", columnList = "purchase_order_id"),
                @Index(name = "idx_asn_tenant_vendor", columnList = "tenant_id, vendor_id"),
                @Index(name = "idx_asn_waybill", columnList = "tenant_id, waybill_number")
        }
)
public class VendorAsnShipment {

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
    @JoinColumn(name = "dispatched_by_user_id", nullable = false)
    private VendorUser dispatchedByUser;

    @Column(name = "waybill_number", nullable = false, length = 100)
    private String waybillNumber;

    @Column(name = "ettn", length = 100)
    private String ettn;

    @Column(name = "carrier_name", length = 100)
    private String carrierName;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "vehicle_plate", length = 50)
    private String vehiclePlate;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "driver_national_id", length = 255)
    private String driverNationalId;

    @Column(name = "driver_name", length = 150)
    private String driverName;

    @Column(name = "driver_phone", length = 50)
    private String driverPhone;

    @Column(name = "shipment_date", nullable = false)
    private LocalDate shipmentDate;

    @Column(name = "estimated_arrival_date", nullable = false)
    private LocalDate estimatedArrivalDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AsnShipmentStatus status = AsnShipmentStatus.DISPATCHED;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "asnShipment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VendorAsnShipmentLineItem> lineItems = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected VendorAsnShipment() {}

    public VendorAsnShipment(Tenant tenant,
                             PurchaseOrder purchaseOrder,
                             Vendor vendor,
                             VendorUser dispatchedByUser,
                             String waybillNumber,
                             String ettn,
                             String carrierName,
                             String trackingNumber,
                             String vehiclePlate,
                             String driverNationalId,
                             String driverName,
                             String driverPhone,
                             LocalDate shipmentDate,
                             LocalDate estimatedArrivalDate,
                             String notes) {
        this.tenant = tenant;
        this.purchaseOrder = purchaseOrder;
        this.vendor = vendor;
        this.dispatchedByUser = dispatchedByUser;
        this.waybillNumber = waybillNumber;
        this.ettn = ettn;
        this.carrierName = carrierName;
        this.trackingNumber = trackingNumber;
        this.vehiclePlate = vehiclePlate;
        this.driverNationalId = driverNationalId;
        this.driverName = driverName;
        this.driverPhone = driverPhone;
        this.shipmentDate = shipmentDate;
        this.estimatedArrivalDate = estimatedArrivalDate;
        this.notes = notes;
        this.status = AsnShipmentStatus.DISPATCHED;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public void addLineItem(VendorAsnShipmentLineItem lineItem) {
        lineItems.add(lineItem);
        lineItem.setAsnShipment(this);
    }

    public UUID getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public Vendor getVendor() { return vendor; }
    public VendorUser getDispatchedByUser() { return dispatchedByUser; }
    public String getWaybillNumber() { return waybillNumber; }
    public String getEttn() { return ettn; }
    public String getCarrierName() { return carrierName; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getVehiclePlate() { return vehiclePlate; }
    public String getDriverNationalId() { return driverNationalId; }
    public String getDriverName() { return driverName; }
    public String getDriverPhone() { return driverPhone; }
    public LocalDate getShipmentDate() { return shipmentDate; }
    public LocalDate getEstimatedArrivalDate() { return estimatedArrivalDate; }
    public AsnShipmentStatus getStatus() { return status; }
    public void setStatus(AsnShipmentStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public List<VendorAsnShipmentLineItem> getLineItems() { return lineItems; }
    public Instant getCreatedAt() { return createdAt; }
}
