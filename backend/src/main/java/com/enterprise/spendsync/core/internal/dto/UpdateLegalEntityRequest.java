package com.enterprise.spendsync.core.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLegalEntityRequest(
        @NotBlank(message = "Name cannot be empty")
        @Size(max = 255, message = "Name cannot exceed 255 characters")
        String name,

        @Size(max = 100, message = "Tax office cannot exceed 100 characters")
        String taxOffice,

        @NotBlank(message = "Registered address cannot be empty")
        String registeredAddress
) {}
