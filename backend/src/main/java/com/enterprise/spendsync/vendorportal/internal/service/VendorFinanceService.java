package com.enterprise.spendsync.vendorportal.internal.service;

import com.enterprise.spendsync.vendorportal.dto.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface VendorFinanceService {

    InvoicePaymentStatusResponse getInvoicePaymentStatus(UUID invoiceId, UUID vendorUserId);

    List<EarlyPayOfferResponse> getAvailableEarlyPaymentOffers(UUID vendorUserId);

    AcceptEarlyDiscountResponse acceptEarlyPaymentOffer(UUID invoiceId, UUID vendorUserId);

    StatementOfAccountsResponse getStatementOfAccounts(LocalDate startDate, LocalDate endDate, UUID vendorUserId);

    MonthlyReconciliationResponse getMonthlyReconciliation(int year, int month, UUID vendorUserId);

    MonthlyReconciliationResponse approveMonthlyReconciliation(MonthlyReconciliationApprovalRequest request, UUID vendorUserId);

    VendorCatalogProposalResponse submitCatalogProposal(VendorCatalogProposalRequest request, UUID vendorUserId);

    List<VendorCatalogProposalResponse> getVendorCatalogProposals(UUID vendorUserId);
}
