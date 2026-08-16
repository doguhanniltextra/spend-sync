package com.enterprise.spendsync.core.internal.dto;

import com.enterprise.spendsync.core.internal.domain.Facility;
import com.enterprise.spendsync.core.internal.domain.FacilityType;

import java.time.Instant;
import java.util.UUID;

public record FacilityResponse(
        UUID id,
        UUID tenantId,
        UUID legalEntityId,
        String legalEntityName,
        String name,
        String facilityCode,
        FacilityType facilityType,
        String shippingAddress,
        String contactPerson,
        String contactPhone,
        boolean isActive,
        Instant createdAt
) {
    public static FacilityResponse fromEntity(Facility facility) {
        return new FacilityResponse(
                facility.getId(),
                facility.getTenant().getId(),
                facility.getLegalEntity().getId(),
                facility.getLegalEntity().getName(),
                facility.getName(),
                facility.getFacilityCode(),
                facility.getFacilityType(),
                facility.getShippingAddress(),
                facility.getContactPerson(),
                facility.getContactPhone(),
                facility.isActive(),
                facility.getCreatedAt()
        );
    }
}
