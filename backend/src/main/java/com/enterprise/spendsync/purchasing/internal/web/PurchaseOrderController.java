package com.enterprise.spendsync.purchasing.internal.web;

import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderStatus;
import com.enterprise.spendsync.purchasing.internal.dto.CancelPurchaseOrderRequest;
import com.enterprise.spendsync.purchasing.internal.dto.CreatePurchaseOrderRequest;
import com.enterprise.spendsync.purchasing.internal.dto.PORevisionResponse;
import com.enterprise.spendsync.purchasing.internal.dto.PurchaseOrderDetailResponse;
import com.enterprise.spendsync.purchasing.internal.dto.PurchaseOrderSummaryResponse;
import com.enterprise.spendsync.purchasing.internal.dto.RevisePurchaseOrderRequest;
import com.enterprise.spendsync.purchasing.internal.service.PurchaseOrderService;
import com.enterprise.spendsync.shared.config.Endpoints;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping(Endpoints.Purchasing.ORDERS_BASE)
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_PO_CREATE')")
    public ResponseEntity<PurchaseOrderDetailResponse> createPurchaseOrder(
            @Valid @RequestBody CreatePurchaseOrderRequest request) {
        PurchaseOrderDetailResponse response = purchaseOrderService.createPurchaseOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(Endpoints.Purchasing.ORDER_BY_ID)
    @PreAuthorize("hasAnyAuthority('PERM_PO_READ', 'PERM_PO_CREATE')")
    public ResponseEntity<PurchaseOrderDetailResponse> getPurchaseOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrderById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PERM_PO_READ', 'PERM_PO_CREATE')")
    public ResponseEntity<List<PurchaseOrderSummaryResponse>> getAllPurchaseOrders(
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) UUID vendorId) {
        return ResponseEntity.ok(purchaseOrderService.getAllPurchaseOrders(status, vendorId));
    }

    @PostMapping(Endpoints.Purchasing.ORDER_ISSUE)
    @PreAuthorize("hasAnyAuthority('PERM_PO_CREATE', 'PERM_PO_UPDATE')")
    public ResponseEntity<PurchaseOrderDetailResponse> issuePurchaseOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseOrderService.issuePurchaseOrder(id));
    }

    @PostMapping(Endpoints.Purchasing.ORDER_REVISE)
    @PreAuthorize("hasAuthority('PERM_PO_UPDATE')")
    public ResponseEntity<PurchaseOrderDetailResponse> revisePurchaseOrder(
            @PathVariable UUID id,
            @Valid @RequestBody RevisePurchaseOrderRequest request) {
        return ResponseEntity.ok(purchaseOrderService.revisePurchaseOrder(id, request));
    }

    @GetMapping(Endpoints.Purchasing.ORDER_REVISIONS)
    @PreAuthorize("hasAnyAuthority('PERM_PO_READ', 'PERM_PO_CREATE')")
    public ResponseEntity<List<PORevisionResponse>> getPurchaseOrderRevisions(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrderRevisions(id));
    }

    @PostMapping(Endpoints.Purchasing.ORDER_CANCEL)
    @PreAuthorize("hasAnyAuthority('PERM_PO_CREATE', 'PERM_PO_UPDATE')")
    public ResponseEntity<PurchaseOrderDetailResponse> cancelPurchaseOrder(
            @PathVariable UUID id,
            @Valid @RequestBody CancelPurchaseOrderRequest request) {
        return ResponseEntity.ok(purchaseOrderService.cancelPurchaseOrder(id, request));
    }
}
