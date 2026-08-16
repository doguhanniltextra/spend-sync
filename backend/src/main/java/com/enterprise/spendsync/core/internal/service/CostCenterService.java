package com.enterprise.spendsync.core.internal.service;

import com.enterprise.spendsync.core.internal.dto.CostCenterResponse;
import com.enterprise.spendsync.core.internal.dto.CreateCostCenterRequest;
import com.enterprise.spendsync.core.internal.dto.UpdateCostCenterRequest;

import java.util.List;
import java.util.UUID;

public interface CostCenterService {
    List<CostCenterResponse> getAllCostCenters(UUID legalEntityId);
    CostCenterResponse getCostCenterById(UUID id);
    CostCenterResponse createCostCenter(CreateCostCenterRequest request);
    CostCenterResponse updateCostCenter(UUID id, UpdateCostCenterRequest request);
    CostCenterResponse updateStatus(UUID id, boolean isActive);
}
