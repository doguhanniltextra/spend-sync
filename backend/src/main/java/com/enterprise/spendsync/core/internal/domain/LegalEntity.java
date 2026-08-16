package com.enterprise.spendsync.core.internal.domain;

import com.enterprise.spendsync.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * LegalEntity represents a registered corporate tax unit and commercial contracting entity.
 */
@Entity
@Table(name = "legal_entities")
public class LegalEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "company_code", nullable = false, length = 20)
    private String companyCode;

    @Column(name = "tax_number", nullable = false, length = 50)
    private String taxNumber;

    @Column(name = "tax_office", length = 100)
    private String taxOffice;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @Column(name = "registered_address", nullable = false, columnDefinition = "TEXT")
    private String registeredAddress;

    @Column(name = "country", nullable = false, length = 2)
    private String country;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public LegalEntity() {
    }

    public LegalEntity(Tenant tenant, String name, String companyCode, String taxNumber, String baseCurrency, String registeredAddress, String country) {
        this.tenant = tenant;
        this.name = name;
        this.companyCode = companyCode;
        this.taxNumber = taxNumber;
        this.baseCurrency = baseCurrency;
        this.registeredAddress = registeredAddress;
        this.country = country;
        this.isActive = true;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getTaxNumber() {
        return taxNumber;
    }

    public void setTaxNumber(String taxNumber) {
        this.taxNumber = taxNumber;
    }

    public String getTaxOffice() {
        return taxOffice;
    }

    public void setTaxOffice(String taxOffice) {
        this.taxOffice = taxOffice;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public String getRegisteredAddress() {
        return registeredAddress;
    }

    public void setRegisteredAddress(String registeredAddress) {
        this.registeredAddress = registeredAddress;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
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
        LegalEntity that = (LegalEntity) o;
        return Objects.equals(companyCode, that.companyCode) && Objects.equals(taxNumber, that.taxNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), companyCode, taxNumber);
    }

    @Override
    public String toString() {
        return "LegalEntity{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", companyCode='" + companyCode + '\'' +
                ", taxNumber='" + taxNumber + '\'' +
                ", baseCurrency='" + baseCurrency + '\'' +
                ", country='" + country + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
