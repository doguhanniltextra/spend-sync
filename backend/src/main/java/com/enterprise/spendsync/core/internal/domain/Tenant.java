package com.enterprise.spendsync.core.internal.domain;

import com.enterprise.spendsync.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * Tenant represents the outermost customer account and isolation boundary.
 */
@Entity
@Table(name = "tenants")
public class Tenant extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "subscription_tier", nullable = false, length = 50)
    private String subscriptionTier = "ENTERPRISE";

    public Tenant() {
    }

    public Tenant(String name, String slug) {
        this.name = name;
        this.slug = slug;
        this.isActive = true;
        this.subscriptionTier = "ENTERPRISE";
    }

    public Tenant(UUID id, String name, String slug, boolean isActive, String subscriptionTier) {
        super(id);
        this.name = name;
        this.slug = slug;
        this.isActive = isActive;
        this.subscriptionTier = subscriptionTier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getSubscriptionTier() {
        return subscriptionTier;
    }

    public void setSubscriptionTier(String subscriptionTier) {
        this.subscriptionTier = subscriptionTier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Tenant tenant = (Tenant) o;
        return Objects.equals(slug, tenant.slug);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), slug);
    }

    @Override
    public String toString() {
        return "Tenant{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", slug='" + slug + '\'' +
                ", isActive=" + isActive +
                ", subscriptionTier='" + subscriptionTier + '\'' +
                '}';
    }
}
