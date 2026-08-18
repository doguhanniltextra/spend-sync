package com.enterprise.spendsync.purchasing.internal.domain;

/**
 * Operational status of a vendor master record.
 */
public enum VendorStatus {
    ACTIVE,             // Sipariş verilebilir
    BLOCKED,            // Geçici olarak askıya alındı
    INACTIVE            // Pasif
}
