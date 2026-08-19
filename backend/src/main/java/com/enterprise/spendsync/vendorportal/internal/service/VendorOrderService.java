package com.enterprise.spendsync.vendorportal.internal.service;

import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderStatus;
import com.enterprise.spendsync.vendorportal.dto.VendorAsnDispatchRequest;
import com.enterprise.spendsync.vendorportal.dto.VendorAsnResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorOrderDetailResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorOrderSummaryResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorPoAcknowledgmentRequest;

import java.util.List;
import java.util.UUID;

public interface VendorOrderService {

    List<VendorOrderSummaryResponse> getVendorOrders(PurchaseOrderStatus status, UUID vendorUserId);

    VendorOrderDetailResponse getVendorOrderDetail(UUID orderId, UUID vendorUserId);

    VendorOrderDetailResponse.AcknowledgmentDto acknowledgeOrder(UUID orderId, VendorPoAcknowledgmentRequest request, UUID vendorUserId);

    VendorAsnResponse dispatchAsnShipment(UUID orderId, VendorAsnDispatchRequest request, UUID vendorUserId);

    List<VendorAsnResponse> getOrderAsnShipments(UUID orderId, UUID vendorUserId);
}
