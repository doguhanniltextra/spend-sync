package com.enterprise.spendsync.core.internal.service;

import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.dto.CostCenterResponse;
import com.enterprise.spendsync.core.internal.dto.CreateCostCenterRequest;
import com.enterprise.spendsync.core.internal.dto.UpdateCostCenterRequest;
import com.enterprise.spendsync.core.internal.repository.CostCenterRepository;
import com.enterprise.spendsync.core.internal.repository.LegalEntityRepository;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CostCenterServiceImpl implements CostCenterService {

    private final CostCenterRepository costCenterRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    public CostCenterServiceImpl(CostCenterRepository costCenterRepository,
                                 LegalEntityRepository legalEntityRepository,
                                 TenantRepository tenantRepository,
                                 UserRepository userRepository) {
        this.costCenterRepository = costCenterRepository;
        this.legalEntityRepository = legalEntityRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CostCenterResponse> getAllCostCenters(UUID legalEntityId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        if (legalEntityId != null) {
            return costCenterRepository.findAllByLegalEntityId(legalEntityId).stream()
                    .filter(c -> c.getTenant().getId().equals(tenantId))
                    .map(CostCenterResponse::fromEntity)
                    .toList();
        }
        return costCenterRepository.findAllByTenantId(tenantId).stream()
                .map(CostCenterResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CostCenterResponse getCostCenterById(UUID id) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        CostCenter costCenter = findCostCenterOrThrow(id, tenantId);
        return CostCenterResponse.fromEntity(costCenter);
    }

    @Override
    public CostCenterResponse createCostCenter(CreateCostCenterRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new SpendSyncException("Tenant not found", HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND") {});

        LegalEntity legalEntity = legalEntityRepository.findById(request.legalEntityId())
                .orElseThrow(() -> new SpendSyncException("Legal entity not found", HttpStatus.NOT_FOUND, "LEGAL_ENTITY_NOT_FOUND") {});

        if (!legalEntity.getTenant().getId().equals(tenantId)) {
            throw new SpendSyncException("Legal entity does not belong to active tenant.", HttpStatus.FORBIDDEN, "CROSS_TENANT_ACCESS_DENIED") {};
        }

        if (costCenterRepository.existsByTenantIdAndCode(tenantId, request.code().trim())) {
            throw new SpendSyncException("Cost center with code '" + request.code() + "' already exists in this tenant.",
                    HttpStatus.CONFLICT, "COST_CENTER_CODE_ALREADY_EXISTS") {};
        }

        CostCenter costCenter = new CostCenter(
                tenant,
                legalEntity,
                request.code().trim().toUpperCase(),
                request.name().trim()
        );

        if (request.managerUserId() != null) {
            User manager = userRepository.findById(request.managerUserId())
                    .orElseThrow(() -> new SpendSyncException("Manager user not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND") {});
            if (manager.getTenant() == null || !manager.getTenant().getId().equals(tenantId)) {
                throw new SpendSyncException("Manager user does not belong to the active tenant.", HttpStatus.FORBIDDEN, "CROSS_TENANT_ACCESS_DENIED") {};
            }
            costCenter.setManagerUser(manager);
        }

        CostCenter saved = costCenterRepository.save(costCenter);
        return CostCenterResponse.fromEntity(saved);
    }

    @Override
    public CostCenterResponse updateCostCenter(UUID id, UpdateCostCenterRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        CostCenter costCenter = findCostCenterOrThrow(id, tenantId);

        costCenter.setName(request.name().trim());

        if (request.managerUserId() != null) {
            User manager = userRepository.findById(request.managerUserId())
                    .orElseThrow(() -> new SpendSyncException("Manager user not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND") {});
            if (manager.getTenant() == null || !manager.getTenant().getId().equals(tenantId)) {
                throw new SpendSyncException("Manager user does not belong to the active tenant.", HttpStatus.FORBIDDEN, "CROSS_TENANT_ACCESS_DENIED") {};
            }
            costCenter.setManagerUser(manager);
        } else {
            costCenter.setManagerUser(null);
        }

        CostCenter updated = costCenterRepository.save(costCenter);
        return CostCenterResponse.fromEntity(updated);
    }

    @Override
    public CostCenterResponse updateStatus(UUID id, boolean isActive) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        CostCenter costCenter = findCostCenterOrThrow(id, tenantId);

        costCenter.setActive(isActive);
        CostCenter updated = costCenterRepository.save(costCenter);
        return CostCenterResponse.fromEntity(updated);
    }

    private CostCenter findCostCenterOrThrow(UUID id, UUID tenantId) {
        CostCenter costCenter = costCenterRepository.findById(id)
                .orElseThrow(() -> new SpendSyncException("Cost center with id '" + id + "' was not found.", HttpStatus.NOT_FOUND, "COST_CENTER_NOT_FOUND") {});

        if (!costCenter.getTenant().getId().equals(tenantId)) {
            throw new SpendSyncException("Cost center with id '" + id + "' does not belong to active tenant.", HttpStatus.NOT_FOUND, "COST_CENTER_NOT_FOUND") {};
        }

        return costCenter;
    }
}
