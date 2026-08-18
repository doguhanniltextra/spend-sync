package com.enterprise.spendsync.payment.internal.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentItemPayload(
        UUID invoiceId,
        String invoiceNumber,
        UUID vendorId,
        String vendorName,
        String vendorIban,
        BigDecimal netPayableAmount
) {}
