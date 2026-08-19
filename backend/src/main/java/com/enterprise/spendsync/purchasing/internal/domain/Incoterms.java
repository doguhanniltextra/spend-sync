package com.enterprise.spendsync.purchasing.internal.domain;

/**
 * International Commercial Terms (ICC Incoterms® 2020 standard).
 */
public enum Incoterms {
    DAP,  // Delivered at Place (Destination delivery)
    DDP,  // Delivered Duty Paid (Customs duties paid import standard)
    EXW,  // Ex Works (Factory pickup by buyer)
    FOB,  // Free on Board (Port shipment)
    CIF,  // Cost, Insurance and Freight (Maritime shipment with insurance)
    CPT   // Carriage Paid To (Carriage paid delivery)
}
