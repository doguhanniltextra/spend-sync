package com.enterprise.spendsync.core.internal.domain;

import com.enterprise.spendsync.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Facility represents a physical delivery location, warehouse dock, or office receiving desk.
 */
@Entity
@Table(name = "facilities")
public class Facility extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    private LegalEntity legalEntity;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "facility_code", nullable = false, length = 50)
    private String facilityCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "facility_type", nullable = false, length = 50)
    private FacilityType facilityType;

    @Column(name = "shipping_address", nullable = false, columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "contact_person", length = 150)
    private String contactPerson;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public Facility() {
    }

    public Facility(Tenant tenant, LegalEntity legalEntity, String name, String facilityCode, FacilityType facilityType, String shippingAddress) {
        this.tenant = tenant;
        this.legalEntity = legalEntity;
        this.name = name;
        this.facilityCode = facilityCode;
        this.facilityType = facilityType;
        this.shippingAddress = shippingAddress;
        this.isActive = true;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public LegalEntity getLegalEntity() {
        return legalEntity;
    }

    public void setLegalEntity(LegalEntity legalEntity) {
        this.legalEntity = legalEntity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFacilityCode() {
        return facilityCode;
    }

    public void setFacilityCode(String facilityCode) {
        this.facilityCode = facilityCode;
    }

    public FacilityType getFacilityType() {
        return facilityType;
    }

    public void setFacilityType(FacilityType facilityType) {
        this.facilityType = facilityType;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Facility facility = (Facility) o;
        return Objects.equals(facilityCode, facility.facilityCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), facilityCode);
    }

    @Override
    public String toString() {
        return "Facility{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", facilityCode='" + facilityCode + '\'' +
                ", facilityType=" + facilityType +
                ", isActive=" + isActive +
                '}';
    }
}
