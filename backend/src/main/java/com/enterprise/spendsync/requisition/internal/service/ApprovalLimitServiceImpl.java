package com.enterprise.spendsync.requisition.internal.service;

import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.repository.CostCenterRepository;
import com.enterprise.spendsync.core.internal.repository.LegalEntityRepository;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.requisition.internal.domain.ApprovalAuthorityLimit;
import com.enterprise.spendsync.requisition.internal.dto.ApprovalLimitResponse;
import com.enterprise.spendsync.requisition.internal.dto.SetApprovalLimitRequest;
import com.enterprise.spendsync.requisition.internal.repository.ApprovalAuthorityLimitRepository;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ApprovalLimitServiceImpl implements ApprovalLimitService {

    private final ApprovalAuthorityLimitRepository limitRepository;
    private final UserRepository userRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final CostCenterRepository costCenterRepository;
    private final TenantRepository tenantRepository;

    public ApprovalLimitServiceImpl(ApprovalAuthorityLimitRepository limitRepository,
                                    UserRepository userRepository,
                                    LegalEntityRepository legalEntityRepository,
                                    CostCenterRepository costCenterRepository,
                                    TenantRepository tenantRepository) {
        this.limitRepository = limitRepository;
        this.userRepository = userRepository;
        this.legalEntityRepository = legalEntityRepository;
        this.costCenterRepository = costCenterRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public ApprovalLimitResponse setApprovalLimit(SetApprovalLimitRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new SpendSyncException("Tenant not found", HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND") {});

        User user = userRepository.findByIdAndTenantId(request.userId(), tenantId)
                .orElseThrow(() -> new SpendSyncException("User not found in active tenant", HttpStatus.NOT_FOUND, "USER_NOT_FOUND") {});

        LegalEntity legalEntity = legalEntityRepository.findByIdAndTenantId(request.legalEntityId(), tenantId)
                .orElseThrow(() -> new SpendSyncException("Legal Entity not found in active tenant", HttpStatus.NOT_FOUND, "LEGAL_ENTITY_NOT_FOUND") {});

        CostCenter costCenter = null;
        if (request.costCenterId() != null) {
            costCenter = costCenterRepository.findByIdAndTenantId(request.costCenterId(), tenantId)
                    .orElseThrow(() -> new SpendSyncException("Cost Center not found in active tenant", HttpStatus.NOT_FOUND, "COST_CENTER_NOT_FOUND") {});

            if (!costCenter.getLegalEntity().getId().equals(legalEntity.getId())) {
                throw new SpendSyncException("Cost Center does not belong to the specified Legal Entity.",
                        HttpStatus.BAD_REQUEST, "COST_CENTER_LEGAL_ENTITY_MISMATCH") {};
            }
        }

        // Upsert resolution
        Optional<ApprovalAuthorityLimit> existingLimit = request.costCenterId() != null
                ? limitRepository.findByUserIdAndLegalEntityIdAndCostCenterIdAndTenantId(user.getId(), legalEntity.getId(), request.costCenterId(), tenantId)
                : limitRepository.findByUserIdAndLegalEntityIdAndCostCenterIsNullAndTenantId(user.getId(), legalEntity.getId(), tenantId);

        ApprovalAuthorityLimit limit;
        if (existingLimit.isPresent()) {
            limit = existingLimit.get();
            limit.setApprovalLevel(request.approvalLevel());
            limit.setMinAmount(request.minAmount() != null ? request.minAmount() : BigDecimal.ZERO);
            limit.setMaxAmount(request.maxAmount());
            limit.setCurrency(request.currency().toUpperCase());
            limit.setActive(true);
        } else {
            limit = new ApprovalAuthorityLimit(
                    tenant,
                    user,
                    legalEntity,
                    costCenter,
                    request.approvalLevel(),
                    request.minAmount(),
                    request.maxAmount(),
                    request.currency(),
                    true
            );
        }

        // Deactivate any previous user limit configured for the exact same scope and level
        if (costCenter != null) {
            limitRepository.findAllByLegalEntityIdAndCostCenterIdAndApprovalLevelAndTenantId(legalEntity.getId(), costCenter.getId(), request.approvalLevel(), tenantId)
                    .forEach(other -> {
                        if (!other.getUser().getId().equals(user.getId())) {
                            other.setActive(false);
                            limitRepository.save(other);
                        }
                    });
        } else {
            limitRepository.findAllByLegalEntityIdAndCostCenterIsNullAndApprovalLevelAndTenantId(legalEntity.getId(), request.approvalLevel(), tenantId)
                    .forEach(other -> {
                        if (!other.getUser().getId().equals(user.getId())) {
                            other.setActive(false);
                            limitRepository.save(other);
                        }
                    });
        }

        ApprovalAuthorityLimit saved = limitRepository.save(limit);
        return ApprovalLimitResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalLimitResponse getApprovalLimitById(UUID id) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        ApprovalAuthorityLimit limit = limitRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new SpendSyncException("Approval Authority Limit not found", HttpStatus.NOT_FOUND, "APPROVAL_LIMIT_NOT_FOUND") {});
        return ApprovalLimitResponse.from(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalLimitResponse> getAllLimits(UUID legalEntityId, UUID userId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        List<ApprovalAuthorityLimit> limits;

        if (legalEntityId != null && userId != null) {
            limits = limitRepository.findAllByTenantIdAndLegalEntityId(tenantId, legalEntityId).stream()
                    .filter(l -> l.getUser().getId().equals(userId))
                    .toList();
        } else if (legalEntityId != null) {
            limits = limitRepository.findAllByTenantIdAndLegalEntityId(tenantId, legalEntityId);
        } else if (userId != null) {
            limits = limitRepository.findAllByTenantIdAndUserId(tenantId, userId);
        } else {
            limits = limitRepository.findAllByTenantId(tenantId);
        }

        return limits.stream().map(ApprovalLimitResponse::from).toList();
    }

    @Override
    public ApprovalLimitResponse toggleLimitStatus(UUID id, boolean active) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        ApprovalAuthorityLimit limit = limitRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new SpendSyncException("Approval Authority Limit not found", HttpStatus.NOT_FOUND, "APPROVAL_LIMIT_NOT_FOUND") {});

        limit.setActive(active);
        ApprovalAuthorityLimit updated = limitRepository.save(limit);
        return ApprovalLimitResponse.from(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> getEffectiveMaxLimit(UUID userId, UUID legalEntityId, UUID costCenterId) {
        return getEffectiveLimitDetails(userId, legalEntityId, costCenterId)
                .map(ApprovalAuthorityLimit::getMaxAmount);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ApprovalAuthorityLimit> getEffectiveLimitDetails(UUID userId, UUID legalEntityId, UUID costCenterId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        List<ApprovalAuthorityLimit> limits = limitRepository.findEffectiveLimits(userId, legalEntityId, costCenterId, tenantId);
        return limits.isEmpty() ? Optional.empty() : Optional.of(limits.get(0));
    }
}
