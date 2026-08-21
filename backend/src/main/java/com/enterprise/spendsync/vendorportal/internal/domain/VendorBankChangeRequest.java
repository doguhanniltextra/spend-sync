package com.enterprise.spendsync.vendorportal.internal.domain;

import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
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
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vendor_bank_change_requests")
public class VendorBankChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private VendorUser requestedByUser;

    @Column(name = "proposed_bank_name", nullable = false, length = 100)
    private String proposedBankName;

    @jakarta.persistence.Convert(converter = com.enterprise.spendsync.shared.crypto.EncryptedStringConverter.class)
    @Column(name = "proposed_iban", nullable = false, length = 255)
    private String proposedIban;

    @Column(name = "supporting_document_url", length = 500)
    private String supportingDocumentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private BankChangeRequestStatus status = BankChangeRequestStatus.PENDING_REVIEW;

    @Column(name = "reviewed_by_user_id")
    private UUID reviewedByUserId;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public VendorBankChangeRequest() {}

    public VendorBankChangeRequest(Tenant tenant,
                                   Vendor vendor,
                                   VendorUser requestedByUser,
                                   String proposedBankName,
                                   String proposedIban,
                                   String supportingDocumentUrl) {
        this.tenant = tenant;
        this.vendor = vendor;
        this.requestedByUser = requestedByUser;
        this.proposedBankName = proposedBankName;
        this.proposedIban = proposedIban;
        this.supportingDocumentUrl = supportingDocumentUrl;
        this.status = BankChangeRequestStatus.PENDING_REVIEW;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Tenant getTenant() { return tenant; }
    public Vendor getVendor() { return vendor; }
    public VendorUser getRequestedByUser() { return requestedByUser; }
    public String getProposedBankName() { return proposedBankName; }
    public String getProposedIban() { return proposedIban; }
    public String getSupportingDocumentUrl() { return supportingDocumentUrl; }
    public BankChangeRequestStatus getStatus() { return status; }
    public void setStatus(BankChangeRequestStatus status) { this.status = status; }
    public UUID getReviewedByUserId() { return reviewedByUserId; }
    public void setReviewedByUserId(UUID reviewedByUserId) { this.reviewedByUserId = reviewedByUserId; }
    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
