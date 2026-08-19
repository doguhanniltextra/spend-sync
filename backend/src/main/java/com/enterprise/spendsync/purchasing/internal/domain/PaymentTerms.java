package com.enterprise.spendsync.purchasing.internal.domain;

/**
 * Standard commercial invoice payment terms.
 */
public enum PaymentTerms {
    IMMEDIATE,          // Immediate / Cash on Delivery
    NET_15,             // 15 Days Net
    NET_30,             // 30 Days Net (Enterprise Standard)
    NET_45,             // 45 Days Net
    NET_60,             // 60 Days Net
    NET_90,             // 90 Days Net
    CASH_IN_ADVANCE     // Cash in Advance / Prepayment
}
