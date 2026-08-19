package com.enterprise.spendsync.vendorportal.internal.domain;

import com.enterprise.spendsync.catalog.internal.domain.CatalogItem;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "vendor_catalog_proposals",
        indexes = {
                @Index(name = "idx_catalog_proposal_vendor", columnList = "tenant_id, vendor_id"),
                @Index(name = "idx_catalog_proposal_item", columnList = "catalog_item_id")
        }
)
public class VendorCatalogProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_item_id")
    private CatalogItem catalogItem;

    @Column(name = "proposed_item_code", nullable = false, length = 100)
    private String proposedItemCode;

    @Column(name = "proposed_name", nullable = false, length = 255)
    private String proposedName;

    @Column(name = "proposed_category", nullable = false, length = 100)
    private String proposedCategory;

    @Column(name = "proposed_unit_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal proposedUnitPrice;

    @Column(name = "proposed_currency", nullable = false, length = 3)
    private String proposedCurrency = "TRY";

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate = new BigDecimal("20.00");

    @Column(name = "lead_time_days", nullable = false)
    private Integer leadTimeDays = 3;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private VendorCatalogProposalStatus status = VendorCatalogProposalStatus.PENDING_BUYER_REVIEW;

    @Column(name = "buyer_decision_notes", columnDefinition = "TEXT")
    private String buyerDecisionNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedByUser;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected VendorCatalogProposal() {}

    public VendorCatalogProposal(Tenant tenant,
                                 Vendor vendor,
                                 CatalogItem catalogItem,
                                 String proposedItemCode,
                                 String proposedName,
                                 String proposedCategory,
                                 BigDecimal proposedUnitPrice,
                                 String proposedCurrency,
                                 BigDecimal vatRate,
                                 Integer leadTimeDays,
                                 String notes) {
        this.tenant = tenant;
        this.vendor = vendor;
        this.catalogItem = catalogItem;
        this.proposedItemCode = proposedItemCode;
        this.proposedName = proposedName;
        this.proposedCategory = proposedCategory;
        this.proposedUnitPrice = proposedUnitPrice;
        this.proposedCurrency = proposedCurrency != null ? proposedCurrency : "TRY";
        this.vatRate = vatRate != null ? vatRate : new BigDecimal("20.00");
        this.leadTimeDays = leadTimeDays != null ? leadTimeDays : 3;
        this.notes = notes;
        this.status = VendorCatalogProposalStatus.PENDING_BUYER_REVIEW;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public void approve(User user, String decisionNotes) {
        this.status = VendorCatalogProposalStatus.APPROVED;
        this.reviewedByUser = user;
        this.buyerDecisionNotes = decisionNotes;
        this.reviewedAt = Instant.now();
    }

    public void reject(User user, String decisionNotes) {
        this.status = VendorCatalogProposalStatus.REJECTED;
        this.reviewedByUser = user;
        this.buyerDecisionNotes = decisionNotes;
        this.reviewedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public Vendor getVendor() { return vendor; }
    public CatalogItem getCatalogItem() { return catalogItem; }
    public String getProposedItemCode() { return proposedItemCode; }
    public String getProposedName() { return proposedName; }
    public String getProposedCategory() { return proposedCategory; }
    public BigDecimal getProposedUnitPrice() { return proposedUnitPrice; }
    public String getProposedCurrency() { return proposedCurrency; }
    public BigDecimal getVatRate() { return vatRate; }
    public Integer getLeadTimeDays() { return leadTimeDays; }
    public String getNotes() { return notes; }
    public VendorCatalogProposalStatus getStatus() { return status; }
    public String getBuyerDecisionNotes() { return buyerDecisionNotes; }
    public User getReviewedByUser() { return reviewedByUser; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
