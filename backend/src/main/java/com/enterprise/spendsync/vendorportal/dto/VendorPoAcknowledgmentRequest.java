package com.enterprise.spendsync.vendorportal.dto;

import com.enterprise.spendsync.vendorportal.internal.domain.VendorPoAcknowledgmentStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record VendorPoAcknowledgmentRequest(
        @NotNull(message = "Acknowledgment status is required")
        VendorPoAcknowledgmentStatus status,

        LocalDate promisedDeliveryDate,
        String vendorNotes
) {}
