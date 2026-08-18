package com.enterprise.spendsync.requisition.internal.service;

import com.enterprise.spendsync.requisition.internal.domain.ApprovalAuthorityLimit;
import com.enterprise.spendsync.requisition.internal.dto.ApprovalLimitResponse;
import com.enterprise.spendsync.requisition.internal.dto.SetApprovalLimitRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalLimitService {

    /**
     * Upserts an approval authority limit for the specified user, legal entity and optional cost center.
     */
    ApprovalLimitResponse setApprovalLimit(SetApprovalLimitRequest request);

    ApprovalLimitResponse getApprovalLimitById(UUID id);

    List<ApprovalLimitResponse> getAllLimits(UUID legalEntityId, UUID userId);

    ApprovalLimitResponse toggleLimitStatus(UUID id, boolean active);

    /**
     * Resolves the effective maximum signing threshold for a user in the given scope.
     * Returns {@code Optional.empty()} if user has unlimited signing power (e.g. CFO) or if no limit is configured.
     * Use {@link #getEffectiveLimitDetails(UUID, UUID, UUID)} to distinguish between unlimited and unconfigured.
     */
    Optional<BigDecimal> getEffectiveMaxLimit(UUID userId, UUID legalEntityId, UUID costCenterId);

    /**
     * Returns the full effective limit definition matching the most specific scope
     * (Cost Center level first, then Legal Entity level).
     */
    Optional<ApprovalAuthorityLimit> getEffectiveLimitDetails(UUID userId, UUID legalEntityId, UUID costCenterId);
}
