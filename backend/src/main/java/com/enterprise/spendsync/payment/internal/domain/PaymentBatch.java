package com.enterprise.spendsync.payment.internal.domain;

import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "payment_batches",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_batch_number", columnNames = {"tenant_id", "batch_number"}),
                @UniqueConstraint(name = "uk_payment_batch_idempotency", columnNames = {"tenant_id", "idempotency_key"})
        },
        indexes = {
                @Index(name = "idx_payment_batches_tenant_created", columnList = "tenant_id, created_at DESC")
        }
)
public class PaymentBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "batch_number", nullable = false, length = 50)
    private String batchNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    private LegalEntity legalEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod = PaymentMethod.ISO_20022_PAIN_001;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "item_count", nullable = false)
    private int itemCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PaymentBatchStatus status = PaymentBatchStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedByUser;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "xml_payload", columnDefinition = "TEXT")
    private String xmlPayload;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @OneToMany(mappedBy = "paymentBatch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentBatchItem> lineItems = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PaymentBatch() {
    }

    public PaymentBatch(Tenant tenant,
                        String batchNumber,
                        LegalEntity legalEntity,
                        PaymentMethod paymentMethod,
                        BigDecimal totalAmount,
                        String currency,
                        User createdByUser,
                        String idempotencyKey) {
        this.tenant = tenant;
        this.batchNumber = batchNumber;
        this.legalEntity = legalEntity;
        this.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.ISO_20022_PAIN_001;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.createdByUser = createdByUser;
        this.idempotencyKey = idempotencyKey;
        this.status = PaymentBatchStatus.DRAFT;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addLineItem(PaymentBatchItem item) {
        lineItems.add(item);
        item.setPaymentBatch(this);
        this.itemCount = lineItems.size();
    }

    // Getters & Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Tenant getTenant() { return tenant; }
    public String getBatchNumber() { return batchNumber; }
    public LegalEntity getLegalEntity() { return legalEntity; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getCurrency() { return currency; }
    public int getItemCount() { return itemCount; }
    public PaymentBatchStatus getStatus() { return status; }
    public void setStatus(PaymentBatchStatus status) { this.status = status; }
    public User getCreatedByUser() { return createdByUser; }
    public User getApprovedByUser() { return approvedByUser; }
    public void setApprovedByUser(User approvedByUser) { this.approvedByUser = approvedByUser; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public String getXmlPayload() { return xmlPayload; }
    public void setXmlPayload(String xmlPayload) { this.xmlPayload = xmlPayload; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public List<PaymentBatchItem> getLineItems() { return lineItems; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
