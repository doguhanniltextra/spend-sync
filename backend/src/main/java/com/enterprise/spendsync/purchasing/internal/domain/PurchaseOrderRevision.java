package com.enterprise.spendsync.purchasing.internal.domain;

import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Purchase Order Revision Snapshot Entity.
 * Stores previous version details and differential budget tracking upon change orders.
 */
@Entity
@Table(name = "purchase_order_revisions")
public class PurchaseOrderRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "revision_number", nullable = false)
    private int revisionNumber;

    @Column(name = "previous_total_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal previousTotalAmount;

    @Column(name = "new_total_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal newTotalAmount;

    @Column(name = "differential_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal differentialAmount;

    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revised_by_user_id", nullable = false)
    private User revisedByUser;

    @Column(name = "snapshot_payload", columnDefinition = "TEXT")
    private String snapshotPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PurchaseOrderRevision() {
    }

    public PurchaseOrderRevision(Tenant tenant,
                                 PurchaseOrder purchaseOrder,
                                 int revisionNumber,
                                 BigDecimal previousTotalAmount,
                                 BigDecimal newTotalAmount,
                                 BigDecimal differentialAmount,
                                 String reason,
                                 User revisedByUser,
                                 String snapshotPayload) {
        this.tenant = tenant;
        this.purchaseOrder = purchaseOrder;
        this.revisionNumber = revisionNumber;
        this.previousTotalAmount = previousTotalAmount;
        this.newTotalAmount = newTotalAmount;
        this.differentialAmount = differentialAmount;
        this.reason = reason;
        this.revisedByUser = revisedByUser;
        this.snapshotPayload = snapshotPayload;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Getters
    public UUID getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public int getRevisionNumber() { return revisionNumber; }
    public BigDecimal getPreviousTotalAmount() { return previousTotalAmount; }
    public BigDecimal getNewTotalAmount() { return newTotalAmount; }
    public BigDecimal getDifferentialAmount() { return differentialAmount; }
    public String getReason() { return reason; }
    public User getRevisedByUser() { return revisedByUser; }
    public String getSnapshotPayload() { return snapshotPayload; }
    public Instant getCreatedAt() { return createdAt; }
}
