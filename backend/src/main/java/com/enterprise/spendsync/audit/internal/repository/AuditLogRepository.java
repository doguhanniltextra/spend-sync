package com.enterprise.spendsync.audit.internal.repository;

import com.enterprise.spendsync.audit.internal.domain.AuditAction;
import com.enterprise.spendsync.audit.internal.domain.AuditLog;
import com.enterprise.spendsync.audit.internal.domain.ComplianceTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<AuditLog> findAllByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtAsc(UUID tenantId, String entityType, String entityId);

    List<AuditLog> findAllByTenantIdAndCorrelationIdOrderByCreatedAtAsc(UUID tenantId, String correlationId);

    List<AuditLog> findAllByTenantIdAndComplianceTagOrderByCreatedAtDesc(UUID tenantId, ComplianceTag complianceTag);

    List<AuditLog> findAllByTenantIdAndActionOrderByCreatedAtDesc(UUID tenantId, AuditAction action);

    List<AuditLog> findAllByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(UUID tenantId, Instant startDate, Instant endDate);
}
