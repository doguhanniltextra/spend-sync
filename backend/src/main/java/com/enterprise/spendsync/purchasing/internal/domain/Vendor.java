package com.enterprise.spendsync.purchasing.internal.domain;

import com.enterprise.spendsync.core.internal.domain.Tenant;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Vendor Master Entity.
 * Represents an external supplier for procurement (not a system user).
 */
@Entity
@Table(name = "vendors", uniqueConstraints = {
        @UniqueConstraint(name = "uk_vendor_tax_number_tenant", columnNames = {"tenant_id", "tax_number"})
})
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "tax_number", nullable = false, length = 50)
    private String taxNumber;

    @Column(name = "tax_office", length = 100)
    private String taxOffice;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private VendorCategory category = VendorCategory.IT_HARDWARE;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 50)
    private VendorTier tier = VendorTier.TIER_3_STANDARD;

    @Column(name = "is_einvoice_registered", nullable = false)
    private boolean isEInvoiceRegistered = true;

    @Column(name = "order_email", nullable = false, length = 255)
    private String orderEmail;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", nullable = false, length = 10)
    private String country = "TR";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_terms", nullable = false, length = 50)
    private PaymentTerms paymentTerms = PaymentTerms.NET_30;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @jakarta.persistence.Convert(converter = com.enterprise.spendsync.shared.crypto.EncryptedStringConverter.class)
    @Column(name = "iban", length = 255)
    private String iban;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private VendorStatus status = VendorStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Vendor() {
    }

    public Vendor(Tenant tenant,
                  String name,
                  String taxNumber,
                  String taxOffice,
                  VendorCategory category,
                  VendorTier tier,
                  boolean isEInvoiceRegistered,
                  String orderEmail,
                  String phoneNumber,
                  String address,
                  String city,
                  String country,
                  PaymentTerms paymentTerms,
                  String bankName,
                  String iban) {
        this.tenant = tenant;
        this.name = name;
        this.taxNumber = taxNumber;
        this.taxOffice = taxOffice;
        this.category = category != null ? category : VendorCategory.IT_HARDWARE;
        this.tier = tier != null ? tier : VendorTier.TIER_3_STANDARD;
        this.isEInvoiceRegistered = isEInvoiceRegistered;
        this.orderEmail = orderEmail;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.city = city;
        this.country = country != null ? country : "TR";
        this.paymentTerms = paymentTerms != null ? paymentTerms : PaymentTerms.NET_30;
        this.bankName = bankName;
        this.iban = iban;
        this.status = VendorStatus.ACTIVE;
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

    // Getters and Setters
    public UUID getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTaxNumber() { return taxNumber; }
    public void setTaxNumber(String taxNumber) { this.taxNumber = taxNumber; }
    public String getTaxOffice() { return taxOffice; }
    public void setTaxOffice(String taxOffice) { this.taxOffice = taxOffice; }
    public VendorCategory getCategory() { return category; }
    public void setCategory(VendorCategory category) { this.category = category; }
    public VendorTier getTier() { return tier; }
    public void setTier(VendorTier tier) { this.tier = tier; }
    public boolean isEInvoiceRegistered() { return isEInvoiceRegistered; }
    public void setEInvoiceRegistered(boolean EInvoiceRegistered) { isEInvoiceRegistered = EInvoiceRegistered; }
    public String getOrderEmail() { return orderEmail; }
    public void setOrderEmail(String orderEmail) { this.orderEmail = orderEmail; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public PaymentTerms getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(PaymentTerms paymentTerms) { this.paymentTerms = paymentTerms; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }
    public VendorStatus getStatus() { return status; }
    public void setStatus(VendorStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
