package com.enterprise.spendsync.purchasing.internal.dto;

import com.enterprise.spendsync.purchasing.internal.domain.PaymentTerms;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import com.enterprise.spendsync.purchasing.internal.domain.VendorCategory;
import com.enterprise.spendsync.purchasing.internal.domain.VendorStatus;
import com.enterprise.spendsync.purchasing.internal.domain.VendorTier;

import java.time.Instant;
import java.util.UUID;

public record VendorResponse(
        UUID id,
        String name,
        String taxNumber,
        String taxOffice,
        VendorCategory category,
        VendorTier tier,
        boolean isEInvoiceRegistered,
        String orderEmail,
        String phoneNumber,
        String address,
        String city,
        String country,
        PaymentTerms paymentTerms,
        String bankName,
        String iban,
        VendorStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static VendorResponse from(Vendor vendor) {
        return new VendorResponse(
                vendor.getId(),
                vendor.getName(),
                vendor.getTaxNumber(),
                vendor.getTaxOffice(),
                vendor.getCategory(),
                vendor.getTier(),
                vendor.isEInvoiceRegistered(),
                vendor.getOrderEmail(),
                vendor.getPhoneNumber(),
                vendor.getAddress(),
                vendor.getCity(),
                vendor.getCountry(),
                vendor.getPaymentTerms(),
                vendor.getBankName(),
                vendor.getIban(),
                vendor.getStatus(),
                vendor.getCreatedAt(),
                vendor.getUpdatedAt()
        );
    }
}
