package com.enterprise.spendsync.vendorportal.internal.domain;

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
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vendor_invitations")
public class VendorInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "tax_number", nullable = false, length = 50)
    private String taxNumber;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "invitation_token", nullable = false, unique = true, length = 255)
    private String invitationToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private VendorInvitationStatus status = VendorInvitationStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    public VendorInvitation() {}

    public VendorInvitation(Tenant tenant,
                            String email,
                            String taxNumber,
                            String companyName,
                            String invitationToken,
                            Instant expiresAt,
                            UUID createdByUserId) {
        this.tenant = tenant;
        this.email = email;
        this.taxNumber = taxNumber;
        this.companyName = companyName;
        this.invitationToken = invitationToken;
        this.expiresAt = expiresAt;
        this.createdByUserId = createdByUserId;
        this.status = VendorInvitationStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Tenant getTenant() { return tenant; }
    public String getEmail() { return email; }
    public String getTaxNumber() { return taxNumber; }
    public String getCompanyName() { return companyName; }
    public String getInvitationToken() { return invitationToken; }
    public VendorInvitationStatus getStatus() { return status; }
    public void setStatus(VendorInvitationStatus status) { this.status = status; }
    public Instant getExpiresAt() { return expiresAt; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }
}
