package com.enterprise.spendsync.matching.internal.dto;

import jakarta.validation.constraints.NotBlank;

public record ManagerOverrideRequest(
        @NotBlank(message = "Override justification note is mandatory")
        String overrideNote
) {}
