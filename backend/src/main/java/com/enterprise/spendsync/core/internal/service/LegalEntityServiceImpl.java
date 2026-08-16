package com.enterprise.spendsync.core.internal.service;

import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.dto.CreateLegalEntityRequest;
import com.enterprise.spendsync.core.internal.dto.LegalEntityResponse;
import com.enterprise.spendsync.core.internal.dto.UpdateLegalEntityRequest;
import com.enterprise.spendsync.core.internal.repository.LegalEntityRepository;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class LegalEntityServiceImpl implements LegalEntityService {

    private final LegalEntityRepository legalEntityRepository;
    private final TenantRepository tenantRepository;

    public LegalEntityServiceImpl(LegalEntityRepository legalEntityRepository, TenantRepository tenantRepository) {
        this.legalEntityRepository = legalEntityRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LegalEntityResponse> getAllLegalEntities() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return legalEntityRepository.findAllByTenantId(tenantId).stream()
                .map(LegalEntityResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LegalEntityResponse getLegalEntityById(UUID id) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        LegalEntity entity = findLegalEntityOrThrow(id, tenantId);
        return LegalEntityResponse.fromEntity(entity);
    }

    @Override
    public LegalEntityResponse createLegalEntity(CreateLegalEntityRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new SpendSyncException("Tenant not found", HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND") {});

        if (legalEntityRepository.existsByTenantIdAndTaxNumber(tenantId, request.taxNumber().trim())) {
            throw new SpendSyncException("A legal entity with tax number '" + request.taxNumber() + "' already exists in this tenant.",
                    HttpStatus.CONFLICT, "TAX_NUMBER_ALREADY_EXISTS") {};
        }

        LegalEntity legalEntity = new LegalEntity(
                tenant,
                request.name().trim(),
                request.companyCode().trim().toUpperCase(),
                request.taxNumber().trim(),
                request.baseCurrency().trim().toUpperCase(),
                request.registeredAddress().trim(),
                request.country().trim().toUpperCase()
        );
        if (request.taxOffice() != null && !request.taxOffice().isBlank()) {
            legalEntity.setTaxOffice(request.taxOffice().trim());
        }

        LegalEntity saved = legalEntityRepository.save(legalEntity);
        return LegalEntityResponse.fromEntity(saved);
    }

    @Override
    public LegalEntityResponse updateLegalEntity(UUID id, UpdateLegalEntityRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        LegalEntity entity = findLegalEntityOrThrow(id, tenantId);

        entity.setName(request.name().trim());
        entity.setRegisteredAddress(request.registeredAddress().trim());
        if (request.taxOffice() != null) {
            entity.setTaxOffice(request.taxOffice().trim());
        }

        LegalEntity updated = legalEntityRepository.save(entity);
        return LegalEntityResponse.fromEntity(updated);
    }

    @Override
    public LegalEntityResponse updateStatus(UUID id, boolean isActive) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        LegalEntity entity = findLegalEntityOrThrow(id, tenantId);

        entity.setActive(isActive);
        LegalEntity updated = legalEntityRepository.save(entity);
        return LegalEntityResponse.fromEntity(updated);
    }

    private LegalEntity findLegalEntityOrThrow(UUID id, UUID tenantId) {
        LegalEntity entity = legalEntityRepository.findById(id)
                .orElseThrow(() -> new SpendSyncException("Legal entity with id '" + id + "' was not found.", HttpStatus.NOT_FOUND, "LEGAL_ENTITY_NOT_FOUND") {});

        // Strict cross-tenant isolation enforcement
        if (!entity.getTenant().getId().equals(tenantId)) {
            throw new SpendSyncException("Legal entity with id '" + id + "' does not belong to the active tenant.", HttpStatus.NOT_FOUND, "LEGAL_ENTITY_NOT_FOUND") {};
        }

        return entity;
    }
}
