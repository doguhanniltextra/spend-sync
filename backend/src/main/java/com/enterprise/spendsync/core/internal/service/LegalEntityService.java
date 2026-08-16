package com.enterprise.spendsync.core.internal.service;

import com.enterprise.spendsync.core.internal.dto.CreateLegalEntityRequest;
import com.enterprise.spendsync.core.internal.dto.LegalEntityResponse;
import com.enterprise.spendsync.core.internal.dto.UpdateLegalEntityRequest;

import java.util.List;
import java.util.UUID;

public interface LegalEntityService {
    List<LegalEntityResponse> getAllLegalEntities();
    LegalEntityResponse getLegalEntityById(UUID id);
    LegalEntityResponse createLegalEntity(CreateLegalEntityRequest request);
    LegalEntityResponse updateLegalEntity(UUID id, UpdateLegalEntityRequest request);
    LegalEntityResponse updateStatus(UUID id, boolean isActive);
}
