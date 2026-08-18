package com.enterprise.spendsync.receiving.internal.service;

import com.enterprise.spendsync.receiving.internal.dto.CreateGoodsReceiptRequest;
import com.enterprise.spendsync.receiving.internal.dto.GoodsReceiptResponse;
import com.enterprise.spendsync.receiving.internal.dto.PendingPOForReceivingResponse;

import java.util.List;
import java.util.UUID;

public interface GoodsReceiptService {

    GoodsReceiptResponse createGoodsReceipt(CreateGoodsReceiptRequest request);

    GoodsReceiptResponse getGoodsReceiptById(UUID id);

    List<GoodsReceiptResponse> getGoodsReceiptsByPurchaseOrder(UUID purchaseOrderId);

    List<PendingPOForReceivingResponse> getPendingOrdersForReceiving();
}
