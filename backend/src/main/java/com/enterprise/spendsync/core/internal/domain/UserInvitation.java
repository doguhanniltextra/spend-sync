package com.enterprise.spendsync.core.internal.domain;

import com.enterprise.spendsync.shared.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * UserInvitation represents secure tokens generated for sub-account and requisitioner onboarding.
 */
@Entity
@Table(name = "user_invitations")
public class UserInvitation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "email", length = 255)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_legal_entity_id", nullable = false)
    private LegalEntity targetLegalEntity;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_invitation_roles", joinColumns = @JoinColumn(name = "invitation_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private Set<RoleType> targetRoles = new HashSet<>();

    @Column(name = "invite_token", nullable = false, unique = true, length = 255)
    private String inviteToken;

    @Column(name = "is_multi_use", nullable = false)
    private boolean isMultiUse = false;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "is_accepted", nullable = false)
    private boolean isAccepted = false;

    public UserInvitation() {
    }

    public UserInvitation(Tenant tenant, String email, LegalEntity targetLegalEntity, String inviteToken, boolean isMultiUse, Instant expiresAt) {
        this.tenant = tenant;
        this.email = email;
        this.targetLegalEntity = targetLegalEntity;
        this.inviteToken = inviteToken;
        this.isMultiUse = isMultiUse;
        this.expiresAt = expiresAt;
        this.isAccepted = false;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !isExpired() && (isMultiUse || !isAccepted);
    }

    public void addTargetRole(RoleType role) {
        this.targetRoles.add(role);
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LegalEntity getTargetLegalEntity() {
        return targetLegalEntity;
    }

    public void setTargetLegalEntity(LegalEntity targetLegalEntity) {
        this.targetLegalEntity = targetLegalEntity;
    }

    public Set<RoleType> getTargetRoles() {
        return targetRoles;
    }

    public void setTargetRoles(Set<RoleType> targetRoles) {
        this.targetRoles = targetRoles;
    }

    public String getInviteToken() {
        return inviteToken;
    }

    public void setInviteToken(String inviteToken) {
        this.inviteToken = inviteToken;
    }

    public boolean isMultiUse() {
        return isMultiUse;
    }

    public void setMultiUse(boolean multiUse) {
        isMultiUse = multiUse;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isAccepted() {
        return isAccepted;
    }

    public void setAccepted(boolean accepted) {
        isAccepted = accepted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UserInvitation that = (UserInvitation) o;
        return Objects.equals(inviteToken, that.inviteToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), inviteToken);
    }

    @Override
    public String toString() {
        return "UserInvitation{" +
                "id=" + getId() +
                ", email='" + email + '\'' +
                ", isMultiUse=" + isMultiUse +
                ", isAccepted=" + isAccepted +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
