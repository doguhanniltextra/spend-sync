package com.enterprise.spendsync.purchasing.internal.service;

import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderStatus;
import com.enterprise.spendsync.purchasing.internal.dto.CancelPurchaseOrderRequest;
import com.enterprise.spendsync.purchasing.internal.dto.CreatePurchaseOrderRequest;
import com.enterprise.spendsync.purchasing.internal.dto.PORevisionResponse;
import com.enterprise.spendsync.purchasing.internal.dto.PurchaseOrderDetailResponse;
import com.enterprise.spendsync.purchasing.internal.dto.PurchaseOrderSummaryResponse;
import com.enterprise.spendsync.purchasing.internal.dto.RevisePurchaseOrderRequest;

import java.util.List;
import java.util.UUID;

public interface PurchaseOrderService {

    PurchaseOrderDetailResponse createPurchaseOrder(CreatePurchaseOrderRequest request);

    PurchaseOrderDetailResponse getPurchaseOrderById(UUID poId);

    List<PurchaseOrderSummaryResponse> getAllPurchaseOrders(PurchaseOrderStatus status, UUID vendorId);

    PurchaseOrderDetailResponse issuePurchaseOrder(UUID poId);

    PurchaseOrderDetailResponse revisePurchaseOrder(UUID poId, RevisePurchaseOrderRequest request);

    List<PORevisionResponse> getPurchaseOrderRevisions(UUID poId);

    PurchaseOrderDetailResponse cancelPurchaseOrder(UUID poId, CancelPurchaseOrderRequest request);
}
