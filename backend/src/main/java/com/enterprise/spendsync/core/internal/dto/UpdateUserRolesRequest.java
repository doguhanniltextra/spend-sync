package com.enterprise.spendsync.core.internal.dto;

import com.enterprise.spendsync.core.internal.domain.RoleType;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record UpdateUserRolesRequest(
        @NotEmpty(message = "At least one role must be assigned")
        Set<RoleType> roles
) {}
