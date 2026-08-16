package com.enterprise.spendsync.core.internal.service;

import com.enterprise.spendsync.core.internal.dto.CreateFacilityRequest;
import com.enterprise.spendsync.core.internal.dto.FacilityResponse;
import com.enterprise.spendsync.core.internal.dto.UpdateFacilityRequest;

import java.util.List;
import java.util.UUID;

public interface FacilityService {
    List<FacilityResponse> getAllFacilities(UUID legalEntityId);
    FacilityResponse getFacilityById(UUID id);
    FacilityResponse createFacility(CreateFacilityRequest request);
    FacilityResponse updateFacility(UUID id, UpdateFacilityRequest request);
    FacilityResponse updateStatus(UUID id, boolean isActive);
}
