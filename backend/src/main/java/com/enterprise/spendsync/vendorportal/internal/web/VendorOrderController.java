package com.enterprise.spendsync.vendorportal.internal.web;

import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderStatus;
import com.enterprise.spendsync.shared.config.Endpoints;
import com.enterprise.spendsync.shared.security.UserPrincipal;
import com.enterprise.spendsync.vendorportal.dto.VendorAsnDispatchRequest;
import com.enterprise.spendsync.vendorportal.dto.VendorAsnResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorOrderDetailResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorOrderSummaryResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorPoAcknowledgmentRequest;
import com.enterprise.spendsync.vendorportal.internal.service.VendorOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Endpoints.VendorPortal.ORDERS_BASE)
@PreAuthorize("hasAnyRole('VENDOR_ADMIN', 'VENDOR_CONTACT')")
public class VendorOrderController {

    private final VendorOrderService vendorOrderService;

    public VendorOrderController(VendorOrderService vendorOrderService) {
        this.vendorOrderService = vendorOrderService;
    }

    @GetMapping
    public ResponseEntity<List<VendorOrderSummaryResponse>> getVendorOrders(
            @RequestParam(required = false) PurchaseOrderStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<VendorOrderSummaryResponse> orders = vendorOrderService.getVendorOrders(status, principal.getId());
        return ResponseEntity.ok(orders);
    }

    @GetMapping(Endpoints.VendorPortal.ORDER_BY_ID)
    public ResponseEntity<VendorOrderDetailResponse> getVendorOrderDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        VendorOrderDetailResponse orderDetail = vendorOrderService.getVendorOrderDetail(id, principal.getId());
        return ResponseEntity.ok(orderDetail);
    }

    @PostMapping(Endpoints.VendorPortal.ORDER_ACKNOWLEDGE)
    public ResponseEntity<VendorOrderDetailResponse.AcknowledgmentDto> acknowledgeOrder(
            @PathVariable UUID id,
            @Valid @RequestBody VendorPoAcknowledgmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        VendorOrderDetailResponse.AcknowledgmentDto ack = vendorOrderService.acknowledgeOrder(id, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ack);
    }

    @PostMapping(Endpoints.VendorPortal.ORDER_DISPATCH)
    public ResponseEntity<VendorAsnResponse> dispatchAsnShipment(
            @PathVariable UUID id,
            @Valid @RequestBody VendorAsnDispatchRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        VendorAsnResponse response = vendorOrderService.dispatchAsnShipment(id, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(Endpoints.VendorPortal.ORDER_ASNS)
    public ResponseEntity<List<VendorAsnResponse>> getOrderAsnShipments(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<VendorAsnResponse> asns = vendorOrderService.getOrderAsnShipments(id, principal.getId());
        return ResponseEntity.ok(asns);
    }
}
