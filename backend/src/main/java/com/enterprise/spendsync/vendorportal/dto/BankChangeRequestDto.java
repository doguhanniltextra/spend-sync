package com.enterprise.spendsync.vendorportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class BankChangeRequestDto {

    public record Submission(
            @NotBlank(message = "Proposed bank name is required")
            @Size(max = 100)
            String proposedBankName,

            @NotBlank(message = "Proposed IBAN is required")
            @Size(max = 50)
            String proposedIban,

            String supportingDocumentUrl
    ) {}

    public record Response(
            UUID id,
            UUID vendorId,
            String vendorName,
            String proposedBankName,
            String proposedIban,
            String maskedProposedIban,
            String supportingDocumentUrl,
            String status,
            String requestedByFullName,
            String requestedByEmail,
            UUID reviewedByUserId,
            String reviewNotes,
            Instant reviewedAt,
            Instant createdAt
    ) {}
}
