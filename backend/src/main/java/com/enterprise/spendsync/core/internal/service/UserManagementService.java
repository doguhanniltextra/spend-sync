package com.enterprise.spendsync.core.internal.service;

import com.enterprise.spendsync.core.internal.dto.UpdateUserLegalEntitiesRequest;
import com.enterprise.spendsync.core.internal.dto.UpdateUserRolesRequest;
import com.enterprise.spendsync.core.internal.dto.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserManagementService {
    List<UserResponse> getAllUsers();
    UserResponse getUserById(UUID id);
    UserResponse updateUserRoles(UUID id, UpdateUserRolesRequest request);
    UserResponse updateUserLegalEntities(UUID id, UpdateUserLegalEntitiesRequest request);
    UserResponse updateStatus(UUID id, boolean isActive);
}
