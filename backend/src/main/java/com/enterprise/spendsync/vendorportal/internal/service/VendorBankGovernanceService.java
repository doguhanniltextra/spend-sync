package com.enterprise.spendsync.vendorportal.internal.service;

import com.enterprise.spendsync.vendorportal.dto.BankChangeDecisionRequest;
import com.enterprise.spendsync.vendorportal.dto.BankChangeRequestDto;

import java.util.List;
import java.util.UUID;

public interface VendorBankGovernanceService {

    BankChangeRequestDto.Response submitBankChangeRequest(BankChangeRequestDto.Submission request, UUID vendorUserId);

    List<BankChangeRequestDto.Response> getVendorBankChangeRequests(UUID vendorUserId);

    List<BankChangeRequestDto.Response> listPendingBankChangeRequests();

    BankChangeRequestDto.Response approveBankChangeRequest(UUID requestId, BankChangeDecisionRequest request, UUID reviewerUserId);

    BankChangeRequestDto.Response rejectBankChangeRequest(UUID requestId, BankChangeDecisionRequest request, UUID reviewerUserId);
}
