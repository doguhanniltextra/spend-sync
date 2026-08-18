package com.enterprise.spendsync.budget.internal.domain;

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
import java.util.UUID;

/**
 * Immutable audit ledger entry recording every balance change in a Budget Pool.
 */
@Entity
@Table(
        name = "budget_transactions",
        indexes = {
                @Index(name = "idx_budget_tx_pool_id", columnList = "budget_pool_id"),
                @Index(name = "idx_budget_tx_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_budget_tx_reference", columnList = "reference_id, reference_type")
        }
)
public class BudgetTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_pool_id", nullable = false)
    private BudgetPool budgetPool;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private BudgetTransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 18, scale = 4)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 18, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    protected BudgetTransaction() {
        super();
    }

    public BudgetTransaction(BudgetPool budgetPool,
                             Tenant tenant,
                             BudgetTransactionType transactionType,
                             BigDecimal amount,
                             BigDecimal balanceBefore,
                             BigDecimal balanceAfter,
                             UUID referenceId,
                             String referenceType,
                             String notes) {
        super();
        this.budgetPool = budgetPool;
        this.tenant = tenant;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.notes = notes;
    }

    public BudgetPool getBudgetPool() {
        return budgetPool;
    }

    public void setBudgetPool(BudgetPool budgetPool) {
        this.budgetPool = budgetPool;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public BudgetTransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(BudgetTransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public void setBalanceBefore(BigDecimal balanceBefore) {
        this.balanceBefore = balanceBefore;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(UUID referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
