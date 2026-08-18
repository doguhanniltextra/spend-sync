package com.enterprise.spendsync.purchasing.internal.dto;

import com.enterprise.spendsync.purchasing.internal.domain.PaymentTerms;
import com.enterprise.spendsync.purchasing.internal.domain.VendorCategory;
import com.enterprise.spendsync.purchasing.internal.domain.VendorTier;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateVendorRequest(
        @NotBlank(message = "Vendor name is mandatory")
        @Size(max = 255)
        String name,

        @NotBlank(message = "Tax number (VKN/TCKN) is mandatory")
        @Size(max = 50)
        String taxNumber,

        String taxOffice,

        VendorCategory category,

        VendorTier tier,

        Boolean isEInvoiceRegistered,

        @NotBlank(message = "Order email address is mandatory")
        @Email(message = "Invalid order email format")
        String orderEmail,

        String phoneNumber,
        String address,
        String city,
        String country,

        @NotNull(message = "Payment terms are mandatory")
        PaymentTerms paymentTerms,

        String bankName,
        String iban
) {}
