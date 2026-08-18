package com.enterprise.spendsync.budget.internal.domain;

import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * BudgetPool represents a dedicated departmental budget allocation for a specific
 * cost center, legal entity, and fiscal period.
 */
@Entity
@Table(
        name = "budget_pools",
        indexes = {
                @Index(name = "idx_budget_pools_tenant_year", columnList = "tenant_id, fiscal_year"),
                @Index(name = "idx_budget_pools_cc_year_period", columnList = "cost_center_id, fiscal_year, period_type, period_value", unique = true),
                @Index(name = "idx_budget_pools_legal_entity", columnList = "legal_entity_id"),
                @Index(name = "idx_budget_pools_status", columnList = "status")
        }
)
public class BudgetPool extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    private LegalEntity legalEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cost_center_id", nullable = false)
    private CostCenter costCenter;

    @Column(name = "fiscal_year", nullable = false)
    private int fiscalYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20)
    private BudgetPeriodType periodType = BudgetPeriodType.ANNUAL;

    @Column(name = "period_value", nullable = false, length = 20)
    private String periodValue = "ANNUAL";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BudgetStatus status = BudgetStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "enforcement_mode", nullable = false, length = 20)
    private BudgetEnforcementMode enforcementMode = BudgetEnforcementMode.HARD_STOP;

    @Column(name = "tolerance_percentage", precision = 5, scale = 2)
    private BigDecimal tolerancePercentage = BigDecimal.ZERO;

    @Column(name = "allocated_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal allocatedAmount = BigDecimal.ZERO;

    @Column(name = "reserved_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal reservedAmount = BigDecimal.ZERO;

    @Column(name = "spent_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal spentAmount = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    protected BudgetPool() {
        super();
    }

    public BudgetPool(Tenant tenant,
                      LegalEntity legalEntity,
                      CostCenter costCenter,
                      int fiscalYear,
                      BudgetPeriodType periodType,
                      String periodValue,
                      BudgetStatus status,
                      BudgetEnforcementMode enforcementMode,
                      BigDecimal tolerancePercentage,
                      BigDecimal allocatedAmount,
                      String currency) {
        super();
        this.tenant = tenant;
        this.legalEntity = legalEntity;
        this.costCenter = costCenter;
        this.fiscalYear = fiscalYear;
        this.periodType = periodType != null ? periodType : BudgetPeriodType.ANNUAL;
        this.periodValue = periodValue != null ? periodValue : this.periodType.name();
        this.status = status != null ? status : BudgetStatus.ACTIVE;
        this.enforcementMode = enforcementMode != null ? enforcementMode : BudgetEnforcementMode.HARD_STOP;
        this.tolerancePercentage = tolerancePercentage != null ? tolerancePercentage : BigDecimal.ZERO;
        this.allocatedAmount = allocatedAmount != null ? allocatedAmount : BigDecimal.ZERO;
        this.reservedAmount = BigDecimal.ZERO;
        this.spentAmount = BigDecimal.ZERO;
        this.currency = currency != null ? currency.toUpperCase() : "TRY";
    }

    /**
     * Computes the currently available funds: Allocated - (Reserved + Spent).
     */
    public BigDecimal getAvailableAmount() {
        return allocatedAmount.subtract(reservedAmount.add(spentAmount));
    }

    /**
     * Computes maximum allowed spend including configured tolerance percentage.
     */
    public BigDecimal getMaxAllowedAllocation() {
        if (enforcementMode == BudgetEnforcementMode.TOLERANCE && tolerancePercentage != null && tolerancePercentage.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal multiplier = BigDecimal.ONE.add(tolerancePercentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            return allocatedAmount.multiply(multiplier);
        }
        return allocatedAmount;
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

    public CostCenter getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(CostCenter costCenter) {
        this.costCenter = costCenter;
    }

    public int getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(int fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public BudgetPeriodType getPeriodType() {
        return periodType;
    }

    public void setPeriodType(BudgetPeriodType periodType) {
        this.periodType = periodType;
    }

    public String getPeriodValue() {
        return periodValue;
    }

    public void setPeriodValue(String periodValue) {
        this.periodValue = periodValue;
    }

    public BudgetStatus getStatus() {
        return status;
    }

    public void setStatus(BudgetStatus status) {
        this.status = status;
    }

    public BudgetEnforcementMode getEnforcementMode() {
        return enforcementMode;
    }

    public void setEnforcementMode(BudgetEnforcementMode enforcementMode) {
        this.enforcementMode = enforcementMode;
    }

    public BigDecimal getTolerancePercentage() {
        return tolerancePercentage;
    }

    public void setTolerancePercentage(BigDecimal tolerancePercentage) {
        this.tolerancePercentage = tolerancePercentage;
    }

    public BigDecimal getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(BigDecimal allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }

    public BigDecimal getReservedAmount() {
        return reservedAmount;
    }

    public void setReservedAmount(BigDecimal reservedAmount) {
        this.reservedAmount = reservedAmount;
    }

    public BigDecimal getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(BigDecimal spentAmount) {
        this.spentAmount = spentAmount;
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
        if (!(o instanceof BudgetPool that)) return false;
        if (!super.equals(o)) return false;
        return fiscalYear == that.fiscalYear &&
                Objects.equals(tenant, that.tenant) &&
                Objects.equals(costCenter, that.costCenter) &&
                periodType == that.periodType &&
                Objects.equals(periodValue, that.periodValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tenant, costCenter, fiscalYear, periodType, periodValue);
    }

    @Override
    public String toString() {
        return "BudgetPool{" +
                "id=" + getId() +
                ", fiscalYear=" + fiscalYear +
                ", periodType=" + periodType +
                ", periodValue='" + periodValue + '\'' +
                ", status=" + status +
                ", enforcementMode=" + enforcementMode +
                ", allocatedAmount=" + allocatedAmount +
                ", reservedAmount=" + reservedAmount +
                ", spentAmount=" + spentAmount +
                ", available=" + getAvailableAmount() +
                ", currency='" + currency + '\'' +
                '}';
    }
}
