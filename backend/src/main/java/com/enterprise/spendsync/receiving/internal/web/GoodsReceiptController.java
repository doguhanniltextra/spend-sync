package com.enterprise.spendsync.receiving.internal.web;

import com.enterprise.spendsync.receiving.internal.dto.CreateGoodsReceiptRequest;
import com.enterprise.spendsync.receiving.internal.dto.GoodsReceiptResponse;
import com.enterprise.spendsync.receiving.internal.dto.PendingPOForReceivingResponse;
import com.enterprise.spendsync.receiving.internal.service.GoodsReceiptService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Endpoints.Receiving.BASE)
public class GoodsReceiptController {

    private final GoodsReceiptService goodsReceiptService;

    public GoodsReceiptController(GoodsReceiptService goodsReceiptService) {
        this.goodsReceiptService = goodsReceiptService;
    }

    @PostMapping(Endpoints.Receiving.RECEIPTS)
    @PreAuthorize("hasAnyAuthority('PERM_GR_CREATE', 'PERM_PO_UPDATE', 'PERM_ORG_MANAGE')")
    public ResponseEntity<GoodsReceiptResponse> createGoodsReceipt(@Valid @RequestBody CreateGoodsReceiptRequest request) {
        GoodsReceiptResponse response = goodsReceiptService.createGoodsReceipt(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(Endpoints.Receiving.RECEIPT_BY_ID)
    @PreAuthorize("hasAnyAuthority('PERM_GR_READ', 'PERM_PO_READ', 'PERM_INVOICE_READ')")
    public ResponseEntity<GoodsReceiptResponse> getGoodsReceiptById(@PathVariable UUID id) {
        return ResponseEntity.ok(goodsReceiptService.getGoodsReceiptById(id));
    }

    @GetMapping(Endpoints.Receiving.RECEIPTS_BY_PO)
    @PreAuthorize("hasAnyAuthority('PERM_GR_READ', 'PERM_PO_READ', 'PERM_INVOICE_READ')")
    public ResponseEntity<List<GoodsReceiptResponse>> getGoodsReceiptsByPurchaseOrder(@PathVariable UUID poId) {
        return ResponseEntity.ok(goodsReceiptService.getGoodsReceiptsByPurchaseOrder(poId));
    }

    @GetMapping(Endpoints.Receiving.PENDING_ORDERS)
    @PreAuthorize("hasAnyAuthority('PERM_GR_READ', 'PERM_GR_CREATE', 'PERM_PO_READ')")
    public ResponseEntity<List<PendingPOForReceivingResponse>> getPendingOrdersForReceiving() {
        return ResponseEntity.ok(goodsReceiptService.getPendingOrdersForReceiving());
    }
}
