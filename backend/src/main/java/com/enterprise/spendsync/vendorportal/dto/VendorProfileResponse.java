package com.enterprise.spendsync.vendorportal.dto;

import java.util.UUID;

public record VendorProfileResponse(
        UUID vendorId,
        UUID vendorUserId,
        String companyName,
        String taxNumber,
        String maskedTaxNumber,
        String taxOffice,
        String category,
        String tier,
        String orderEmail,
        String phoneNumber,
        String address,
        String city,
        String country,
        String paymentTerms,
        String bankName,
        String iban,
        String maskedIban,
        String userEmail,
        String userFullName,
        String userRole
) {}
