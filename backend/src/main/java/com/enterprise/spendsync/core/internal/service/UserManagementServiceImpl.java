package com.enterprise.spendsync.core.internal.service;

import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.dto.UpdateUserLegalEntitiesRequest;
import com.enterprise.spendsync.core.internal.dto.UpdateUserRolesRequest;
import com.enterprise.spendsync.core.internal.dto.UserResponse;
import com.enterprise.spendsync.core.internal.repository.LegalEntityRepository;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final LegalEntityRepository legalEntityRepository;

    public UserManagementServiceImpl(UserRepository userRepository, LegalEntityRepository legalEntityRepository) {
        this.userRepository = userRepository;
        this.legalEntityRepository = legalEntityRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return userRepository.findAllByTenantId(tenantId).stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        User user = findTenantUserOrThrow(id, tenantId);
        return UserResponse.fromEntity(user);
    }

    @Override
    public UserResponse updateUserRoles(UUID id, UpdateUserRolesRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        User user = findTenantUserOrThrow(id, tenantId);

        user.setRoles(new HashSet<>(request.roles()));
        User updated = userRepository.save(user);
        return UserResponse.fromEntity(updated);
    }

    @Override
    public UserResponse updateUserLegalEntities(UUID id, UpdateUserLegalEntitiesRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        User user = findTenantUserOrThrow(id, tenantId);

        Set<LegalEntity> assignedEntities = new HashSet<>();
        for (UUID entityId : request.legalEntityIds()) {
            LegalEntity entity = legalEntityRepository.findById(entityId)
                    .orElseThrow(() -> new SpendSyncException("Legal entity with id '" + entityId + "' was not found.", HttpStatus.NOT_FOUND, "LEGAL_ENTITY_NOT_FOUND") {});

            if (!entity.getTenant().getId().equals(tenantId)) {
                throw new SpendSyncException("Legal entity with id '" + entityId + "' does not belong to active tenant.", HttpStatus.FORBIDDEN, "CROSS_TENANT_ACCESS_DENIED") {};
            }
            assignedEntities.add(entity);
        }

        user.setAssignedLegalEntities(assignedEntities);
        User updated = userRepository.save(user);
        return UserResponse.fromEntity(updated);
    }

    @Override
    public UserResponse updateStatus(UUID id, boolean isActive) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        User user = findTenantUserOrThrow(id, tenantId);

        user.setActive(isActive);
        User updated = userRepository.save(user);
        return UserResponse.fromEntity(updated);
    }

    private User findTenantUserOrThrow(UUID id, UUID tenantId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new SpendSyncException("User with id '" + id + "' was not found.", HttpStatus.NOT_FOUND, "USER_NOT_FOUND") {});

        if (user.getTenant() == null || !user.getTenant().getId().equals(tenantId)) {
            throw new SpendSyncException("User does not belong to the active tenant.", HttpStatus.NOT_FOUND, "USER_NOT_FOUND") {};
        }

        return user;
    }
}
