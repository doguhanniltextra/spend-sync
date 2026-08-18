package com.enterprise.spendsync.requisition.internal.domain;

import com.enterprise.spendsync.budget.internal.domain.BudgetPool;
import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.Facility;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.shared.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * PurchaseRequisition (PR) represents a formal purchase request submitted by an authorized
 * employee, routed through a dynamic approval DAG, and backed by a reserved budget pool.
 */
@Entity
@Table(
        name = "purchase_requisitions",
        indexes = {
                @Index(name = "idx_prs_tenant_number", columnList = "tenant_id, requisition_number", unique = true),
                @Index(name = "idx_prs_requisitioner", columnList = "requisitioner_id"),
                @Index(name = "idx_prs_cost_center", columnList = "cost_center_id"),
                @Index(name = "idx_prs_status", columnList = "status"),
                @Index(name = "idx_prs_legal_entity", columnList = "legal_entity_id")
        }
)
public class PurchaseRequisition extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "requisition_number", nullable = false, length = 30)
    private String requisitionNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requisitioner_id", nullable = false)
    private User requisitioner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    private LegalEntity legalEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cost_center_id", nullable = false)
    private CostCenter costCenter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_facility_id", nullable = false)
    private Facility deliveryFacility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_pool_id")
    private BudgetPool budgetPool;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RequisitionStatus status = RequisitionStatus.DRAFT;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "TRY";

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "justification", columnDefinition = "TEXT", nullable = false)
    private String justification;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @OneToMany(mappedBy = "requisition", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    private List<RequisitionLineItem> lineItems = new ArrayList<>();

    @OneToMany(mappedBy = "requisition", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stepOrder ASC")
    private List<RequisitionApprovalStep> approvalSteps = new ArrayList<>();

    protected PurchaseRequisition() {
        super();
    }

    public PurchaseRequisition(Tenant tenant,
                               String requisitionNumber,
                               User requisitioner,
                               LegalEntity legalEntity,
                               CostCenter costCenter,
                               Facility deliveryFacility,
                               BudgetPool budgetPool,
                               RequisitionStatus status,
                               BigDecimal totalAmount,
                               String currency,
                               String title,
                               String justification) {
        super();
        this.tenant = tenant;
        this.requisitionNumber = requisitionNumber;
        this.requisitioner = requisitioner;
        this.legalEntity = legalEntity;
        this.costCenter = costCenter;
        this.deliveryFacility = deliveryFacility;
        this.budgetPool = budgetPool;
        this.status = status != null ? status : RequisitionStatus.DRAFT;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.currency = currency != null ? currency.toUpperCase() : "TRY";
        this.title = title;
        this.justification = justification;
    }

    public void addLineItem(RequisitionLineItem item) {
        lineItems.add(item);
        item.setRequisition(this);
        recalculateTotalAmount();
    }

    public void addApprovalStep(RequisitionApprovalStep step) {
        approvalSteps.add(step);
        step.setRequisition(this);
    }

    public void recalculateTotalAmount() {
        this.totalAmount = lineItems.stream()
                .map(RequisitionLineItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public String getRequisitionNumber() {
        return requisitionNumber;
    }

    public void setRequisitionNumber(String requisitionNumber) {
        this.requisitionNumber = requisitionNumber;
    }

    public User getRequisitioner() {
        return requisitioner;
    }

    public void setRequisitioner(User requisitioner) {
        this.requisitioner = requisitioner;
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

    public Facility getDeliveryFacility() {
        return deliveryFacility;
    }

    public void setDeliveryFacility(Facility deliveryFacility) {
        this.deliveryFacility = deliveryFacility;
    }

    public BudgetPool getBudgetPool() {
        return budgetPool;
    }

    public void setBudgetPool(BudgetPool budgetPool) {
        this.budgetPool = budgetPool;
    }

    public RequisitionStatus getStatus() {
        return status;
    }

    public void setStatus(RequisitionStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public List<RequisitionLineItem> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<RequisitionLineItem> lineItems) {
        this.lineItems = lineItems;
    }

    public List<RequisitionApprovalStep> getApprovalSteps() {
        return approvalSteps;
    }

    public void setApprovalSteps(List<RequisitionApprovalStep> approvalSteps) {
        this.approvalSteps = approvalSteps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PurchaseRequisition that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(tenant, that.tenant) && Objects.equals(requisitionNumber, that.requisitionNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tenant, requisitionNumber);
    }
}
