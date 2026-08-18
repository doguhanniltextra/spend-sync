package com.enterprise.spendsync.purchasing.internal.domain;

/**
 * Lifecycle states of a Purchase Order (PO).
 */
public enum PurchaseOrderStatus {
    DRAFT,              // Satın alma uzmanı taslağı hazırlıyor
    ISSUED,             // Tedarikçiye iletildi, kilitlendi, teslimat bekleniyor
    REVISED,            // Revizyon sürecinde (Geçici durum)
    PARTIALLY_RECEIVED, // Depoya kısmi mal girişi yapıldı (GR)
    FULFILLED,          // Tüm kalemler eksiksiz teslim alındı ve faturalandı
    CANCELLED           // Sipariş iptal edildi (Kalan bütçe rezervasyonu iade edilir)
}
