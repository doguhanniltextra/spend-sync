package com.enterprise.spendsync.vendorportal.internal.service;

import com.enterprise.spendsync.matching.internal.domain.InvoiceStatus;
import com.enterprise.spendsync.vendorportal.dto.PoFlipInvoiceRequest;
import com.enterprise.spendsync.vendorportal.dto.SupplierInvoiceDetailResponse;
import com.enterprise.spendsync.vendorportal.dto.SupplierInvoiceResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface VendorInvoiceService {

    SupplierInvoiceResponse createPoFlipInvoice(UUID poId, PoFlipInvoiceRequest request, UUID vendorUserId);

    SupplierInvoiceResponse uploadUblXmlInvoice(MultipartFile file, UUID vendorUserId);

    List<SupplierInvoiceResponse> getVendorInvoices(InvoiceStatus status, UUID vendorUserId);

    SupplierInvoiceDetailResponse getVendorInvoiceDetail(UUID invoiceId, UUID vendorUserId);

    String getInvoiceHtml(UUID invoiceId, UUID vendorUserId);
}
