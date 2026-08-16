package com.enterprise.spendsync.core.internal.dto;

import com.enterprise.spendsync.core.internal.domain.FacilityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateFacilityRequest(
        @NotNull(message = "Legal entity ID is required")
        UUID legalEntityId,

        @NotBlank(message = "Facility name cannot be empty")
        @Size(max = 255, message = "Facility name cannot exceed 255 characters")
        String name,

        @NotBlank(message = "Facility code cannot be empty")
        @Size(max = 50, message = "Facility code cannot exceed 50 characters")
        String facilityCode,

        @NotNull(message = "Facility type is required")
        FacilityType facilityType,

        @NotBlank(message = "Shipping address cannot be empty")
        String shippingAddress,

        @Size(max = 150, message = "Contact person cannot exceed 150 characters")
        String contactPerson,

        @Size(max = 30, message = "Contact phone cannot exceed 30 characters")
        String contactPhone
) {}
