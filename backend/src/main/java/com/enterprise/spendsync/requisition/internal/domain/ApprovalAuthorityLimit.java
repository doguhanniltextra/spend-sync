package com.enterprise.spendsync.requisition.internal.domain;

import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * ApprovalAuthorityLimit defines the statutory signing threshold and approval tier
 * for an authorized user within a specific legal entity and optional cost center.
 */
@Entity
@Table(
        name = "approval_authority_limits",
        indexes = {
                @Index(name = "idx_approval_limits_tenant_user", columnList = "tenant_id, user_id"),
                @Index(name = "idx_approval_limits_legal_entity", columnList = "legal_entity_id"),
                @Index(name = "idx_approval_limits_cost_center", columnList = "cost_center_id"),
                @Index(name = "idx_approval_limits_unique_scope", columnList = "tenant_id, user_id, legal_entity_id, cost_center_id", unique = true)
        }
)
public class ApprovalAuthorityLimit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    private LegalEntity legalEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @Column(name = "approval_level", nullable = false)
    private int approvalLevel = 1;

    @Column(name = "min_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal minAmount = BigDecimal.ZERO;

    @Column(name = "max_amount", precision = 18, scale = 4)
    private BigDecimal maxAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "TRY";

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    protected ApprovalAuthorityLimit() {
        super();
    }

    public ApprovalAuthorityLimit(Tenant tenant,
                                  User user,
                                  LegalEntity legalEntity,
                                  CostCenter costCenter,
                                  int approvalLevel,
                                  BigDecimal minAmount,
                                  BigDecimal maxAmount,
                                  String currency,
                                  boolean isActive) {
        super();
        this.tenant = tenant;
        this.user = user;
        this.legalEntity = legalEntity;
        this.costCenter = costCenter;
        this.approvalLevel = approvalLevel;
        this.minAmount = minAmount != null ? minAmount : BigDecimal.ZERO;
        this.maxAmount = maxAmount;
        this.currency = currency != null ? currency.toUpperCase() : "TRY";
        this.isActive = isActive;
    }

    /**
     * Checks if this approval limit provides unlimited signing authority (e.g. CFO / Board level).
     */
    public boolean isUnlimited() {
        return maxAmount == null;
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

    public LegalEntity getLegalEntity() {
        return legalEntity;
    }

    public void setLegalEntity(LegalEntity legalEntity) {
        this.legalEntity = legalEntity;
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

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApprovalAuthorityLimit that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(tenant, that.tenant) &&
                Objects.equals(user, that.user) &&
                Objects.equals(legalEntity, that.legalEntity) &&
                Objects.equals(costCenter, that.costCenter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tenant, user, legalEntity, costCenter);
    }

    @Override
    public String toString() {
        return "ApprovalAuthorityLimit{" +
                "id=" + getId() +
                ", userId=" + (user != null ? user.getId() : null) +
                ", legalEntityId=" + (legalEntity != null ? legalEntity.getId() : null) +
                ", costCenterId=" + (costCenter != null ? costCenter.getId() : null) +
                ", level=" + approvalLevel +
                ", min=" + minAmount +
                ", max=" + (maxAmount != null ? maxAmount : "UNLIMITED") +
                ", currency='" + currency + '\'' +
                ", active=" + isActive +
                '}';
    }
}
