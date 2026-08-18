package com.enterprise.spendsync.audit.internal.domain;

import com.enterprise.spendsync.core.internal.domain.Tenant;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Immutable Audit Log Entity (ISO 27001 / SOC 2 Compliance).
 * Strictly APPEND-ONLY: Update and Delete operations are prohibited by design.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_tenant_created", columnList = "tenant_id, created_at DESC"),
        @Index(name = "idx_audit_logs_correlation", columnList = "correlation_id"),
        @Index(name = "idx_audit_logs_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_logs_actor", columnList = "actor_id"),
        @Index(name = "idx_audit_logs_compliance", columnList = "compliance_tag")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 100)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_tag", nullable = false, length = 100)
    private ComplianceTag complianceTag;

    // Actor details
    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "actor_role", length = 100)
    private String actorRole;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    // Target Entity
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType; // e.g. "PURCHASE_REQUISITION", "PURCHASE_ORDER", "BUDGET_POOL"

    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;   // e.g. "PR-2026-00001", UUID

    @Column(name = "legal_entity_id")
    private UUID legalEntityId;

    @Column(name = "cost_center_id")
    private UUID costCenterId;

    // Financial Snapshot
    @Column(name = "amount", precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", length = 10)
    private String currency;

    // State Transition
    @Column(name = "from_status", length = 50)
    private String fromStatus;

    @Column(name = "to_status", length = 50)
    private String toStatus;

    @Column(name = "decision_note", columnDefinition = "TEXT")
    private String decisionNote;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    // Tamper-Evident SHA-256 Checksum
    @Column(name = "checksum", nullable = false, length = 64)
    private String checksum;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    public AuditLog(Tenant tenant,
                    String correlationId,
                    AuditAction action,
                    ComplianceTag complianceTag,
                    UUID actorId,
                    String actorEmail,
                    String actorRole,
                    String ipAddress,
                    String userAgent,
                    String entityType,
                    String entityId,
                    UUID legalEntityId,
                    UUID costCenterId,
                    BigDecimal amount,
                    String currency,
                    String fromStatus,
                    String toStatus,
                    String decisionNote,
                    String payload) {
        this.tenant = tenant;
        this.correlationId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        this.action = action;
        this.complianceTag = complianceTag != null ? complianceTag : ComplianceTag.ISO_27001_LOGGING;
        this.actorId = actorId;
        this.actorEmail = actorEmail;
        this.actorRole = actorRole;
        this.ipAddress = ipAddress != null ? ipAddress : "127.0.0.1";
        this.userAgent = userAgent;
        this.entityType = entityType;
        this.entityId = entityId;
        this.legalEntityId = legalEntityId;
        this.costCenterId = costCenterId;
        this.amount = amount;
        this.currency = currency;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.decisionNote = decisionNote;
        this.payload = payload;
        this.createdAt = Instant.now();
        this.checksum = calculateChecksum();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.checksum == null) {
            this.checksum = calculateChecksum();
        }
    }

    public String calculateChecksum() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String raw = String.format("%s:%s:%s:%s:%s:%s:%s:%s",
                    tenant != null ? tenant.getId() : "null",
                    correlationId,
                    action,
                    entityType,
                    entityId,
                    amount != null ? amount.toPlainString() : "0.00",
                    createdAt != null ? createdAt.toString() : Instant.now().toString(),
                    actorId != null ? actorId.toString() : "SYSTEM"
            );
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    // Getters only (Immutable / Append-only)
    public UUID getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public String getCorrelationId() { return correlationId; }
    public AuditAction getAction() { return action; }
    public ComplianceTag getComplianceTag() { return complianceTag; }
    public UUID getActorId() { return actorId; }
    public String getActorEmail() { return actorEmail; }
    public String getActorRole() { return actorRole; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public UUID getLegalEntityId() { return legalEntityId; }
    public UUID getCostCenterId() { return costCenterId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getFromStatus() { return fromStatus; }
    public String getToStatus() { return toStatus; }
    public String getDecisionNote() { return decisionNote; }
    public String getPayload() { return payload; }
    public String getChecksum() { return checksum; }
    public Instant getCreatedAt() { return createdAt; }
}
