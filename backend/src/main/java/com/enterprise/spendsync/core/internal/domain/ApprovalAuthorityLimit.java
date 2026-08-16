package com.enterprise.spendsync.core.internal.domain;

import com.enterprise.spendsync.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * ApprovalAuthorityLimit configures maximum transaction signing thresholds per user/cost center.
 */
@Entity
@Table(name = "approval_authority_limits")
public class ApprovalAuthorityLimit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @Column(name = "approval_level", nullable = false)
    private int approvalLevel;

    @Column(name = "max_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal maxAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    public ApprovalAuthorityLimit() {
    }

    public ApprovalAuthorityLimit(Tenant tenant, User user, CostCenter costCenter, int approvalLevel, BigDecimal maxAmount, String currency) {
        this.tenant = tenant;
        this.user = user;
        this.costCenter = costCenter;
        this.approvalLevel = approvalLevel;
        this.maxAmount = maxAmount;
        this.currency = currency;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public CostCenter getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(CostCenter costCenter) {
        this.costCenter = costCenter;
    }

    public int getApprovalLevel() {
        return approvalLevel;
    }

    public void setApprovalLevel(int approvalLevel) {
        this.approvalLevel = approvalLevel;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ApprovalAuthorityLimit that = (ApprovalAuthorityLimit) o;
        return approvalLevel == that.approvalLevel &&
                Objects.equals(user, that.user) &&
                Objects.equals(costCenter, that.costCenter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), user, costCenter, approvalLevel);
    }

    @Override
    public String toString() {
        return "ApprovalAuthorityLimit{" +
                "id=" + getId() +
                ", approvalLevel=" + approvalLevel +
                ", maxAmount=" + maxAmount +
                ", currency='" + currency + '\'' +
                '}';
    }
}
