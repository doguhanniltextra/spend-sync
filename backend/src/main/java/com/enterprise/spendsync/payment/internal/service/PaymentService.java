package com.enterprise.spendsync.payment.internal.service;

import com.enterprise.spendsync.payment.internal.dto.ApprovePaymentBatchRequest;
import com.enterprise.spendsync.payment.internal.dto.CreatePaymentBatchRequest;
import com.enterprise.spendsync.payment.internal.dto.DueInvoiceResponse;
import com.enterprise.spendsync.payment.internal.dto.PaymentBatchResponse;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    List<DueInvoiceResponse> getDueInvoices();

    PaymentBatchResponse createPaymentBatch(CreatePaymentBatchRequest request);

    PaymentBatchResponse getPaymentBatchById(UUID id);

    List<PaymentBatchResponse> getAllPaymentBatches();

    PaymentBatchResponse approveAndDispatchPaymentBatch(UUID id, ApprovePaymentBatchRequest request);

    PaymentBatchResponse cancelPaymentBatch(UUID id);
}
