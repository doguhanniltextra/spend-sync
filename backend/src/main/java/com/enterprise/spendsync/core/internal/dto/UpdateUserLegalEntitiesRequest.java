package com.enterprise.spendsync.core.internal.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;
import java.util.UUID;

public record UpdateUserLegalEntitiesRequest(
        @NotEmpty(message = "At least one legal entity must be assigned")
        Set<UUID> legalEntityIds
) {}
