package com.enterprise.spendsync.core.internal.dto;

import com.enterprise.spendsync.core.internal.domain.Facility;
import com.enterprise.spendsync.core.internal.domain.FacilityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record UpdateFacilityRequest(
        @NotBlank(message = "Facility name cannot be empty")
        @Size(max = 255, message = "Facility name cannot exceed 255 characters")
        String name,

        @NotBlank(message = "Shipping address cannot be empty")
        String shippingAddress,

        @Size(max = 150, message = "Contact person cannot exceed 150 characters")
        String contactPerson,

        @Size(max = 30, message = "Contact phone cannot exceed 30 characters")
        String contactPhone
) {}
