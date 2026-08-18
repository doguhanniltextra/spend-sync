package com.enterprise.spendsync.payment.internal.web;

import com.enterprise.spendsync.payment.internal.dto.ApprovePaymentBatchRequest;
import com.enterprise.spendsync.payment.internal.dto.CreatePaymentBatchRequest;
import com.enterprise.spendsync.payment.internal.dto.DueInvoiceResponse;
import com.enterprise.spendsync.payment.internal.dto.PaymentBatchResponse;
import com.enterprise.spendsync.payment.internal.service.PaymentService;
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
@RequestMapping(Endpoints.Payment.BASE)
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping(Endpoints.Payment.DUE_INVOICES)
    @PreAuthorize("hasAnyAuthority('PERM_PAYMENT_RELEASE', 'PERM_INVOICE_READ', 'PERM_ORG_MANAGE')")
    public ResponseEntity<List<DueInvoiceResponse>> getDueInvoices() {
        return ResponseEntity.ok(paymentService.getDueInvoices());
    }

    @PostMapping(Endpoints.Payment.BATCHES)
    @PreAuthorize("hasAnyAuthority('PERM_PAYMENT_RELEASE', 'PERM_INVOICE_READ', 'PERM_ORG_MANAGE')")
    public ResponseEntity<PaymentBatchResponse> createPaymentBatch(@Valid @RequestBody CreatePaymentBatchRequest request) {
        PaymentBatchResponse response = paymentService.createPaymentBatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(Endpoints.Payment.BATCH_BY_ID)
    @PreAuthorize("hasAnyAuthority('PERM_PAYMENT_RELEASE', 'PERM_INVOICE_READ', 'PERM_ORG_MANAGE')")
    public ResponseEntity<PaymentBatchResponse> getPaymentBatchById(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getPaymentBatchById(id));
    }

    @GetMapping(Endpoints.Payment.BATCHES)
    @PreAuthorize("hasAnyAuthority('PERM_PAYMENT_RELEASE', 'PERM_INVOICE_READ', 'PERM_ORG_MANAGE')")
    public ResponseEntity<List<PaymentBatchResponse>> getAllPaymentBatches() {
        return ResponseEntity.ok(paymentService.getAllPaymentBatches());
    }

    @PostMapping(Endpoints.Payment.APPROVE_BATCH)
    @PreAuthorize("hasAnyAuthority('PERM_PAYMENT_RELEASE', 'PERM_ORG_MANAGE')")
    public ResponseEntity<PaymentBatchResponse> approveAndDispatchPaymentBatch(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ApprovePaymentBatchRequest request) {
        ApprovePaymentBatchRequest req = request != null ? request : new ApprovePaymentBatchRequest("Payment authorized by CFO/Treasury");
        return ResponseEntity.ok(paymentService.approveAndDispatchPaymentBatch(id, req));
    }

    @PostMapping(Endpoints.Payment.CANCEL_BATCH)
    @PreAuthorize("hasAnyAuthority('PERM_PAYMENT_RELEASE', 'PERM_ORG_MANAGE')")
    public ResponseEntity<PaymentBatchResponse> cancelPaymentBatch(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.cancelPaymentBatch(id));
    }
}
