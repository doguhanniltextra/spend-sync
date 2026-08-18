package com.enterprise.spendsync.purchasing.internal.domain;

/**
 * Standard commercial invoice payment terms.
 */
public enum PaymentTerms {
    IMMEDIATE,          // Peşin
    NET_15,             // 15 Gün Vade
    NET_30,             // 30 Gün Vade (Kurumsal Standart)
    NET_45,             // 45 Gün Vade
    NET_60,             // 60 Gün Vade
    NET_90,             // 90 Gün Vade
    CASH_IN_ADVANCE     // Sipariş Öncesi Avans
}
