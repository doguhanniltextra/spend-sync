package com.enterprise.spendsync.core.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCostCenterRequest(
        @NotNull(message = "Legal entity ID is required")
        UUID legalEntityId,

        @NotBlank(message = "Cost center code cannot be empty")
        @Size(max = 50, message = "Cost center code cannot exceed 50 characters")
        String code,

        @NotBlank(message = "Department/Cost center name cannot be empty")
        @Size(max = 255, message = "Name cannot exceed 255 characters")
        String name,

        UUID managerUserId
) {}
