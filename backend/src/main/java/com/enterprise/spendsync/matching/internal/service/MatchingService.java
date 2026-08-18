package com.enterprise.spendsync.matching.internal.service;

import com.enterprise.spendsync.matching.internal.dto.CreateSupplierInvoiceRequest;
import com.enterprise.spendsync.matching.internal.dto.ManagerOverrideRequest;
import com.enterprise.spendsync.matching.internal.dto.RejectInvoiceRequest;
import com.enterprise.spendsync.matching.internal.dto.SupplierInvoiceResponse;

import java.util.List;
import java.util.UUID;

public interface MatchingService {

    SupplierInvoiceResponse createAndEvaluateInvoice(CreateSupplierInvoiceRequest request);

    SupplierInvoiceResponse getInvoiceById(UUID id);

    List<SupplierInvoiceResponse> getInvoicesByPurchaseOrder(UUID purchaseOrderId);

    List<SupplierInvoiceResponse> getAllInvoices();

    SupplierInvoiceResponse managerOverride(UUID id, ManagerOverrideRequest request);

    SupplierInvoiceResponse rejectInvoice(UUID id, RejectInvoiceRequest request);
}
