package com.enterprise.spendsync.core.internal.repository;

import com.enterprise.spendsync.core.internal.domain.ApprovalAuthorityLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalAuthorityLimitRepository extends JpaRepository<ApprovalAuthorityLimit, UUID> {
    List<ApprovalAuthorityLimit> findAllByTenantId(UUID tenantId);
    List<ApprovalAuthorityLimit> findAllByTenantIdAndUserId(UUID tenantId, UUID userId);
}
