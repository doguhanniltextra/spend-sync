package com.enterprise.spendsync.vendorportal.internal.service;

import com.enterprise.spendsync.audit.internal.domain.AuditAction;
import com.enterprise.spendsync.audit.internal.domain.ComplianceTag;
import com.enterprise.spendsync.audit.internal.dto.RecordAuditRequest;
import com.enterprise.spendsync.audit.internal.service.AuditService;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import com.enterprise.spendsync.purchasing.internal.repository.VendorRepository;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import com.enterprise.spendsync.vendorportal.dto.BankChangeDecisionRequest;
import com.enterprise.spendsync.vendorportal.dto.BankChangeRequestDto;
import com.enterprise.spendsync.vendorportal.internal.domain.BankChangeRequestStatus;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorBankChangeRequest;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorUser;
import com.enterprise.spendsync.vendorportal.internal.repository.VendorBankChangeRequestRepository;
import com.enterprise.spendsync.vendorportal.internal.repository.VendorUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VendorBankGovernanceServiceImpl implements VendorBankGovernanceService {

    private final VendorBankChangeRequestRepository bankChangeRequestRepository;
    private final VendorUserRepository vendorUserRepository;
    private final VendorRepository vendorRepository;
    private final AuditService auditService;

    public VendorBankGovernanceServiceImpl(
            VendorBankChangeRequestRepository bankChangeRequestRepository,
            VendorUserRepository vendorUserRepository,
            VendorRepository vendorRepository,
            AuditService auditService) {
        this.bankChangeRequestRepository = bankChangeRequestRepository;
        this.vendorUserRepository = vendorUserRepository;
        this.vendorRepository = vendorRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public BankChangeRequestDto.Response submitBankChangeRequest(BankChangeRequestDto.Submission request, UUID vendorUserId) {
        VendorUser user = vendorUserRepository.findById(vendorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor user not found"));

        Vendor vendor = user.getVendor();

        VendorBankChangeRequest changeRequest = new VendorBankChangeRequest(
                user.getTenant(),
                vendor,
                user,
                request.proposedBankName(),
                request.proposedIban(),
                request.supportingDocumentUrl()
        );

        VendorBankChangeRequest saved = bankChangeRequestRepository.save(changeRequest);

        auditService.recordAuditLog(new RecordAuditRequest(
                UUID.randomUUID().toString(),
                AuditAction.VENDOR_STATUS_CHANGED,
                ComplianceTag.SOX_404_FINANCIAL_CONTROL,
                user.getId(),
                user.getEmail(),
                "VENDOR_ADMIN",
                "127.0.0.1",
                "VendorPortal",
                "VendorBankChangeRequest",
                saved.getId().toString(),
                null,
                null,
                null,
                null,
                null,
                "PENDING_REVIEW",
                "Submitted bank account change request for vendor: " + vendor.getName() + " -> IBAN: " + com.enterprise.spendsync.shared.crypto.MaskingUtils.maskIban(request.proposedIban()),
                "{\"proposedIbanMasked\":\"" + com.enterprise.spendsync.shared.crypto.MaskingUtils.maskIban(request.proposedIban()) + "\",\"proposedBank\":\"" + request.proposedBankName() + "\"}"
        ));

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankChangeRequestDto.Response> getVendorBankChangeRequests(UUID vendorUserId) {
        VendorUser user = vendorUserRepository.findById(vendorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor user not found"));

        return bankChangeRequestRepository
                .findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(user.getTenant().getId(), user.getVendor().getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankChangeRequestDto.Response> listPendingBankChangeRequests() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return bankChangeRequestRepository
                .findAllByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, BankChangeRequestStatus.PENDING_REVIEW)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public BankChangeRequestDto.Response approveBankChangeRequest(UUID requestId, BankChangeDecisionRequest request, UUID reviewerUserId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        VendorBankChangeRequest changeRequest = bankChangeRequestRepository.findById(requestId)
                .filter(r -> r.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bank change request not found"));

        if (changeRequest.getStatus() != BankChangeRequestStatus.PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request has already been processed: " + changeRequest.getStatus());
        }

        Vendor vendor = changeRequest.getVendor();
        String oldIban = vendor.getIban();
        String oldBank = vendor.getBankName();

        // 1. Update active Vendor entity IBAN (Transparently encrypted by EncryptedStringConverter)
        vendor.setIban(changeRequest.getProposedIban());
        vendor.setBankName(changeRequest.getProposedBankName());
        vendorRepository.save(vendor);

        // 2. Mark request APPROVED
        changeRequest.setStatus(BankChangeRequestStatus.APPROVED);
        changeRequest.setReviewedByUserId(reviewerUserId);
        changeRequest.setReviewNotes(request != null ? request.reviewNotes() : "Approved by Finance Officer");
        changeRequest.setReviewedAt(Instant.now());
        VendorBankChangeRequest saved = bankChangeRequestRepository.save(changeRequest);

        auditService.recordAuditLog(new RecordAuditRequest(
                UUID.randomUUID().toString(),
                AuditAction.VENDOR_STATUS_CHANGED,
                ComplianceTag.SOX_404_FINANCIAL_CONTROL,
                reviewerUserId,
                null,
                "FINANCE_OFFICER",
                "127.0.0.1",
                "BuyerPortal",
                "Vendor",
                vendor.getId().toString(),
                null,
                null,
                null,
                null,
                "PENDING_REVIEW",
                "APPROVED",
                "Approved official IBAN change for vendor " + vendor.getName() + " from [" + com.enterprise.spendsync.shared.crypto.MaskingUtils.maskIban(oldIban) + "] to [" + com.enterprise.spendsync.shared.crypto.MaskingUtils.maskIban(changeRequest.getProposedIban()) + "]",
                "{\"oldIbanMasked\":\"" + com.enterprise.spendsync.shared.crypto.MaskingUtils.maskIban(oldIban) + "\",\"newIbanMasked\":\"" + com.enterprise.spendsync.shared.crypto.MaskingUtils.maskIban(changeRequest.getProposedIban()) + "\"}"
        ));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public BankChangeRequestDto.Response rejectBankChangeRequest(UUID requestId, BankChangeDecisionRequest request, UUID reviewerUserId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        VendorBankChangeRequest changeRequest = bankChangeRequestRepository.findById(requestId)
                .filter(r -> r.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bank change request not found"));

        if (changeRequest.getStatus() != BankChangeRequestStatus.PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request has already been processed: " + changeRequest.getStatus());
        }

        changeRequest.setStatus(BankChangeRequestStatus.REJECTED);
        changeRequest.setReviewedByUserId(reviewerUserId);
        changeRequest.setReviewNotes(request != null ? request.reviewNotes() : "Rejected by Finance Officer");
        changeRequest.setReviewedAt(Instant.now());
        VendorBankChangeRequest saved = bankChangeRequestRepository.save(changeRequest);

        auditService.recordAuditLog(new RecordAuditRequest(
                UUID.randomUUID().toString(),
                AuditAction.VENDOR_STATUS_CHANGED,
                ComplianceTag.SOX_404_FINANCIAL_CONTROL,
                reviewerUserId,
                null,
                "FINANCE_OFFICER",
                "127.0.0.1",
                "BuyerPortal",
                "VendorBankChangeRequest",
                saved.getId().toString(),
                null,
                null,
                null,
                null,
                "PENDING_REVIEW",
                "REJECTED",
                "Rejected IBAN change request for vendor " + changeRequest.getVendor().getName() + ". Reason: " + changeRequest.getReviewNotes(),
                "{\"rejectedIbanMasked\":\"" + com.enterprise.spendsync.shared.crypto.MaskingUtils.maskIban(changeRequest.getProposedIban()) + "\",\"reason\":\"" + changeRequest.getReviewNotes() + "\"}"
        ));

        return toResponse(saved);
    }

    private BankChangeRequestDto.Response toResponse(VendorBankChangeRequest r) {
        return new BankChangeRequestDto.Response(
                r.getId(),
                r.getVendor().getId(),
                r.getVendor().getName(),
                r.getProposedBankName(),
                r.getProposedIban(),
                com.enterprise.spendsync.shared.crypto.MaskingUtils.maskIban(r.getProposedIban()),
                r.getSupportingDocumentUrl(),
                r.getStatus().name(),
                r.getRequestedByUser().getFullName(),
                r.getRequestedByUser().getEmail(),
                r.getReviewedByUserId(),
                r.getReviewNotes(),
                r.getReviewedAt(),
                r.getCreatedAt()
        );
    }
}
