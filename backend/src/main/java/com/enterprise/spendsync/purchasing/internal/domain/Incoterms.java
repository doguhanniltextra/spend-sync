package com.enterprise.spendsync.purchasing.internal.domain;

/**
 * International Commercial Terms (ICC Incoterms® 2020 standard).
 */
public enum Incoterms {
    DAP,  // Delivered at Place (Belirlenen Yerde Teslim - En yaygın iç piyasa standardı)
    DDP,  // Delivered Duty Paid (Gümrük Vergileri Ödenmiş Olarak Teslim - İthalat standardı)
    EXW,  // Ex Works (İş Yerinde Teslim - Alıcı kendi aracıyla alır)
    FOB,  // Free on Board (Gemide Masrafsız Teslim)
    CIF,  // Cost, Insurance and Freight (Maliyet, Sigorta ve Navlun)
    CPT   // Carriage Paid To (Taşıma Ücreti Ödenmiş Olarak Teslim)
}
