package com.enterprise.spendsync.purchasing.internal.dto;

import com.enterprise.spendsync.purchasing.internal.domain.VendorStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateVendorStatusRequest(
        @NotNull(message = "Vendor status is mandatory")
        VendorStatus status
) {}
