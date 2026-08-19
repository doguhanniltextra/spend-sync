package com.enterprise.spendsync.matching.internal.service;

import com.enterprise.spendsync.matching.internal.domain.SupplierInvoice;

public interface GibXsltRendererService {

    String renderInvoiceHtml(SupplierInvoice invoice);
}
