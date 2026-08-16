package com.enterprise.spendsync.core.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateCostCenterRequest(
        @NotBlank(message = "Department/Cost center name cannot be empty")
        @Size(max = 255, message = "Name cannot exceed 255 characters")
        String name,

        UUID managerUserId
) {}
