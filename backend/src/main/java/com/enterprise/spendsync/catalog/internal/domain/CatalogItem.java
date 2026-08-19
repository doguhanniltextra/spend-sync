package com.enterprise.spendsync.catalog.internal.domain;

import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import com.enterprise.spendsync.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Enterprise Item Master / Catalog Item.
 * Holds pre-negotiated contracted prices, preferred suppliers, tax codes, and validity windows.
 */
@Entity
@Table(
        name = "catalog_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_item_tenant_code", columnNames = {"tenant_id", "item_code"})
        },
        indexes = {
                @Index(name = "idx_item_tenant", columnList = "tenant_id"),
                @Index(name = "idx_item_category", columnList = "category_id"),
                @Index(name = "idx_item_vendor", columnList = "preferred_vendor_id"),
                @Index(name = "idx_item_active", columnList = "is_active")
        }
)
public class CatalogItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CatalogCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_vendor_id")
    private Vendor preferredVendor;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "TRY";

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate = new BigDecimal("0.20");

    @Column(name = "unit_of_measure", nullable = false, length = 30)
    private String unitOfMeasure = "PIECE";

    @Column(name = "contract_reference", length = 100)
    private String contractReference;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_preferred", nullable = false)
    private boolean isPreferred = false;

    @Column(name = "gl_account_code", length = 50)
    private String glAccountCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    protected CatalogItem() {
        super();
    }

    public CatalogItem(Tenant tenant,
                       String itemCode,
                       String name,
                       String description,
                       CatalogCategory category,
                       Vendor preferredVendor,
                       BigDecimal unitPrice,
                       String currency,
                       BigDecimal vatRate,
                       String unitOfMeasure,
                       String contractReference,
                       LocalDate validFrom,
                       LocalDate validUntil,
                       boolean isPreferred,
                       String glAccountCode,
                       User createdBy) {
        super();
        this.tenant = tenant;
        this.itemCode = itemCode != null ? itemCode.trim().toUpperCase() : "ITEM-" + System.currentTimeMillis();
        this.name = name != null ? name.trim() : "";
        this.description = description;
        this.category = category;
        this.preferredVendor = preferredVendor;
        this.unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        this.currency = currency != null && !currency.isBlank() ? currency : "TRY";
        this.vatRate = vatRate != null ? vatRate : new BigDecimal("0.20");
        this.unitOfMeasure = unitOfMeasure != null && !unitOfMeasure.isBlank() ? unitOfMeasure : "PIECE";
        this.contractReference = contractReference;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.isActive = true;
        this.isPreferred = isPreferred;
        this.glAccountCode = glAccountCode;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public Tenant getTenant() { return tenant; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public CatalogCategory getCategory() { return category; }
    public void setCategory(CatalogCategory category) { this.category = category; }
    public Vendor getPreferredVendor() { return preferredVendor; }
    public void setPreferredVendor(Vendor preferredVendor) { this.preferredVendor = preferredVendor; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getVatRate() { return vatRate; }
    public void setVatRate(BigDecimal vatRate) { this.vatRate = vatRate; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String unitOfMeasure) { this.unitOfMeasure = unitOfMeasure; }
    public String getContractReference() { return contractReference; }
    public void setContractReference(String contractReference) { this.contractReference = contractReference; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public boolean isPreferred() { return isPreferred; }
    public void setPreferred(boolean preferred) { isPreferred = preferred; }
    public String getGlAccountCode() { return glAccountCode; }
    public void setGlAccountCode(String glAccountCode) { this.glAccountCode = glAccountCode; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
}
