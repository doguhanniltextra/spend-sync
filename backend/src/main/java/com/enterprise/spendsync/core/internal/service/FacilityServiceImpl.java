package com.enterprise.spendsync.core.internal.service;

import com.enterprise.spendsync.core.internal.domain.Facility;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.dto.CreateFacilityRequest;
import com.enterprise.spendsync.core.internal.dto.FacilityResponse;
import com.enterprise.spendsync.core.internal.dto.UpdateFacilityRequest;
import com.enterprise.spendsync.core.internal.repository.FacilityRepository;
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
public class FacilityServiceImpl implements FacilityService {

    private final FacilityRepository facilityRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final TenantRepository tenantRepository;

    public FacilityServiceImpl(FacilityRepository facilityRepository,
                               LegalEntityRepository legalEntityRepository,
                               TenantRepository tenantRepository) {
        this.facilityRepository = facilityRepository;
        this.legalEntityRepository = legalEntityRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacilityResponse> getAllFacilities(UUID legalEntityId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        if (legalEntityId != null) {
            return facilityRepository.findAllByLegalEntityId(legalEntityId).stream()
                    .filter(f -> f.getTenant().getId().equals(tenantId))
                    .map(FacilityResponse::fromEntity)
                    .toList();
        }
        return facilityRepository.findAllByTenantId(tenantId).stream()
                .map(FacilityResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FacilityResponse getFacilityById(UUID id) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Facility facility = findFacilityOrThrow(id, tenantId);
        return FacilityResponse.fromEntity(facility);
    }

    @Override
    public FacilityResponse createFacility(CreateFacilityRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new SpendSyncException("Tenant not found", HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND") {});

        LegalEntity legalEntity = legalEntityRepository.findById(request.legalEntityId())
                .orElseThrow(() -> new SpendSyncException("Legal entity not found", HttpStatus.NOT_FOUND, "LEGAL_ENTITY_NOT_FOUND") {});

        if (!legalEntity.getTenant().getId().equals(tenantId)) {
            throw new SpendSyncException("Legal entity does not belong to active tenant.", HttpStatus.FORBIDDEN, "CROSS_TENANT_ACCESS_DENIED") {};
        }

        if (facilityRepository.existsByTenantIdAndFacilityCode(tenantId, request.facilityCode().trim())) {
            throw new SpendSyncException("Facility with code '" + request.facilityCode() + "' already exists in this tenant.",
                    HttpStatus.CONFLICT, "FACILITY_CODE_ALREADY_EXISTS") {};
        }

        Facility facility = new Facility(
                tenant,
                legalEntity,
                request.name().trim(),
                request.facilityCode().trim().toUpperCase(),
                request.facilityType(),
                request.shippingAddress().trim()
        );
        facility.setContactPerson(request.contactPerson() != null ? request.contactPerson().trim() : null);
        facility.setContactPhone(request.contactPhone() != null ? request.contactPhone().trim() : null);

        Facility saved = facilityRepository.save(facility);
        return FacilityResponse.fromEntity(saved);
    }

    @Override
    public FacilityResponse updateFacility(UUID id, UpdateFacilityRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Facility facility = findFacilityOrThrow(id, tenantId);

        facility.setName(request.name().trim());
        facility.setShippingAddress(request.shippingAddress().trim());
        facility.setContactPerson(request.contactPerson() != null ? request.contactPerson().trim() : null);
        facility.setContactPhone(request.contactPhone() != null ? request.contactPhone().trim() : null);

        Facility updated = facilityRepository.save(facility);
        return FacilityResponse.fromEntity(updated);
    }

    @Override
    public FacilityResponse updateStatus(UUID id, boolean isActive) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Facility facility = findFacilityOrThrow(id, tenantId);

        facility.setActive(isActive);
        Facility updated = facilityRepository.save(facility);
        return FacilityResponse.fromEntity(updated);
    }

    private Facility findFacilityOrThrow(UUID id, UUID tenantId) {
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> new SpendSyncException("Facility with id '" + id + "' was not found.", HttpStatus.NOT_FOUND, "FACILITY_NOT_FOUND") {});

        if (!facility.getTenant().getId().equals(tenantId)) {
            throw new SpendSyncException("Facility with id '" + id + "' does not belong to active tenant.", HttpStatus.NOT_FOUND, "FACILITY_NOT_FOUND") {};
        }

        return facility;
    }
}
