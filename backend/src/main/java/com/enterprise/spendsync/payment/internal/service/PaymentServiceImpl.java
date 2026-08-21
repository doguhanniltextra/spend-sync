package com.enterprise.spendsync.payment.internal.service;

import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.repository.LegalEntityRepository;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.matching.internal.domain.InvoiceStatus;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoice;
import com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceRepository;
import com.enterprise.spendsync.payment.internal.domain.PaymentBatch;
import com.enterprise.spendsync.payment.internal.domain.PaymentBatchItem;
import com.enterprise.spendsync.payment.internal.domain.PaymentBatchStatus;
import com.enterprise.spendsync.payment.internal.domain.PaymentMethod;
import com.enterprise.spendsync.payment.internal.dto.ApprovePaymentBatchRequest;
import com.enterprise.spendsync.payment.internal.dto.CreatePaymentBatchRequest;
import com.enterprise.spendsync.payment.internal.dto.DueInvoiceResponse;
import com.enterprise.spendsync.payment.internal.dto.PaymentBatchResponse;
import com.enterprise.spendsync.payment.internal.event.PaymentDispatchedEvent;
import com.enterprise.spendsync.payment.internal.event.PaymentItemPayload;
import com.enterprise.spendsync.payment.internal.repository.PaymentBatchItemRepository;
import com.enterprise.spendsync.payment.internal.repository.PaymentBatchRepository;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.notification.EmailService;
import com.enterprise.spendsync.shared.security.UserPrincipal;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentBatchRepository paymentBatchRepository;
    private final PaymentBatchItemRepository paymentBatchItemRepository;
    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EmailService emailService;

    public PaymentServiceImpl(PaymentBatchRepository paymentBatchRepository,
                              PaymentBatchItemRepository paymentBatchItemRepository,
                              SupplierInvoiceRepository supplierInvoiceRepository,
                              LegalEntityRepository legalEntityRepository,
                              UserRepository userRepository,
                              TenantRepository tenantRepository,
                              ApplicationEventPublisher eventPublisher,
                              EmailService emailService) {
        this.paymentBatchRepository = paymentBatchRepository;
        this.paymentBatchItemRepository = paymentBatchItemRepository;
        this.supplierInvoiceRepository = supplierInvoiceRepository;
        this.legalEntityRepository = legalEntityRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.eventPublisher = eventPublisher;
        this.emailService = emailService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DueInvoiceResponse> getDueInvoices() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return supplierInvoiceRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.APPROVED_FOR_PAYMENT)
                .filter(inv -> !paymentBatchItemRepository.isInvoiceAlreadyInActiveBatch(inv.getId()))
                .map(DueInvoiceResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public PaymentBatchResponse createPaymentBatch(CreatePaymentBatchRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new SpendSyncException("Tenant not found", HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND") {});

        User currentUser = resolveCurrentUser(tenantId);

        String idempotencyKey = request.idempotencyKey() != null ? request.idempotencyKey() : UUID.randomUUID().toString();
        if (paymentBatchRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey).isPresent()) {
            throw new SpendSyncException("A payment batch with this idempotency key already exists: " + idempotencyKey,
                    HttpStatus.CONFLICT, "DUPLICATE_IDEMPOTENCY_KEY") {};
        }

        LegalEntity legalEntity = legalEntityRepository.findByIdAndTenantId(request.legalEntityId(), tenantId)
                .orElseThrow(() -> new SpendSyncException("Legal Entity not found", HttpStatus.NOT_FOUND, "LEGAL_ENTITY_NOT_FOUND") {});

        String batchNumber = generateBatchNumber(tenantId);
        BigDecimal totalBatchAmount = BigDecimal.ZERO;
        String currency = "TRY";

        PaymentBatch batch = new PaymentBatch(
                tenant,
                batchNumber,
                legalEntity,
                request.paymentMethod() != null ? request.paymentMethod() : PaymentMethod.ISO_20022_PAIN_001,
                BigDecimal.ZERO,
                currency,
                currentUser,
                idempotencyKey
        );

        for (UUID invId : request.invoiceIds()) {
            SupplierInvoice invoice = supplierInvoiceRepository.findByIdAndTenantId(invId, tenantId)
                    .orElseThrow(() -> new SpendSyncException("Supplier invoice not found: " + invId, HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND") {});

            if (invoice.getStatus() != InvoiceStatus.APPROVED_FOR_PAYMENT) {
                throw new SpendSyncException("Invoice is not approved for payment: " + invoice.getInvoiceNumber() + " (Status: " + invoice.getStatus() + ")",
                        HttpStatus.BAD_REQUEST, "INVOICE_NOT_APPROVED") {};
            }

            if (paymentBatchItemRepository.isInvoiceAlreadyInActiveBatch(invoice.getId())) {
                throw new SpendSyncException("Invoice is already included in an active payment batch: " + invoice.getInvoiceNumber(),
                        HttpStatus.BAD_REQUEST, "DOUBLE_SPENDING_ATTEMPT_BLOCKED") {};
            }

            currency = invoice.getCurrency();
            BigDecimal amount = invoice.getTotalAmount();
            BigDecimal discount = BigDecimal.ZERO; // Dynamic discounting hook
            BigDecimal netPayable = amount.subtract(discount);

            totalBatchAmount = totalBatchAmount.add(netPayable);

            PaymentBatchItem item = new PaymentBatchItem(
                    tenant,
                    invoice,
                    invoice.getVendor(),
                    invoice.getVendor().getName(),
                    invoice.getVendor().getIban(),
                    amount,
                    discount,
                    netPayable
            );

            batch.addLineItem(item);
        }

        batch.setTotalAmount(totalBatchAmount);

        // Generate ISO 20022 pain.001 XML Mock Payload
        String painXml = generatePain001Xml(batch, legalEntity);
        batch.setXmlPayload(painXml);

        PaymentBatch saved = paymentBatchRepository.save(batch);
        log.info("Payment Batch {} created with {} invoices, total: {} {}", saved.getBatchNumber(), saved.getItemCount(), saved.getTotalAmount(), saved.getCurrency());
        return PaymentBatchResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentBatchResponse getPaymentBatchById(UUID id) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        PaymentBatch batch = paymentBatchRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new SpendSyncException("Payment Batch not found", HttpStatus.NOT_FOUND, "PAYMENT_BATCH_NOT_FOUND") {});
        return PaymentBatchResponse.from(batch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentBatchResponse> getAllPaymentBatches() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return paymentBatchRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(PaymentBatchResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public PaymentBatchResponse approveAndDispatchPaymentBatch(UUID id, ApprovePaymentBatchRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        User currentUser = resolveCurrentUser(tenantId);

        PaymentBatch batch = paymentBatchRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new SpendSyncException("Payment Batch not found", HttpStatus.NOT_FOUND, "PAYMENT_BATCH_NOT_FOUND") {});

        if (batch.getStatus() != PaymentBatchStatus.DRAFT) {
            throw new SpendSyncException("Only DRAFT payment batches can be approved. Current: " + batch.getStatus(),
                    HttpStatus.BAD_REQUEST, "INVALID_BATCH_STATUS") {};
        }

        // SoD (Segregation of Duties) Check: Creator cannot approve their own batch unless ROOT_USER
        boolean isRoot = currentUser.getRoles().contains(RoleType.ROOT_USER);
        if (!isRoot && batch.getCreatedByUser().getId().equals(currentUser.getId())) {
            throw new SpendSyncException("SoD Violation: Batch creator cannot approve their own payment batch. Four-eyes principle required.",
                    HttpStatus.FORBIDDEN, "SOD_VIOLATION_SELF_APPROVAL") {};
        }

        batch.setStatus(PaymentBatchStatus.DISPATCHED);
        batch.setApprovedByUser(currentUser);
        batch.setApprovedAt(Instant.now());

        List<PaymentItemPayload> eventPayloads = new ArrayList<>();

        for (PaymentBatchItem item : batch.getLineItems()) {
            item.setStatus("PAID");

            SupplierInvoice inv = item.getSupplierInvoice();
            inv.setStatus(InvoiceStatus.PAID);
            supplierInvoiceRepository.save(inv);

            eventPayloads.add(new PaymentItemPayload(
                    inv.getId(),
                    inv.getInvoiceNumber(),
                    item.getVendor().getId(),
                    item.getVendorName(),
                    item.getVendorIban(),
                    item.getNetPayableAmount()
            ));

            // Send Remittance Advice Email to Vendor
            String targetEmail = item.getVendor().getOrderEmail();

            if (targetEmail != null && !targetEmail.isBlank()) {
                try {
                    Map<String, Object> emailData = new HashMap<>();
                    emailData.put("vendorName", item.getVendorName());
                    emailData.put("invoiceNumber", inv.getInvoiceNumber());
                    emailData.put("batchNumber", batch.getBatchNumber());
                    emailData.put("amount", item.getNetPayableAmount() + " " + batch.getCurrency());
                    emailData.put("iban", item.getVendorIban());

                    emailService.sendTemplatedEmail(
                            targetEmail,
                            "Payment Remittance Advice: " + inv.getInvoiceNumber() + " - " + batch.getBatchNumber(),
                            "payment-remittance-advice",
                            emailData
                    );
                } catch (Exception ex) {
                    log.warn("Could not dispatch remittance email to vendor {}: {}", targetEmail, ex.getMessage());
                }
            }
        }

        PaymentBatch saved = paymentBatchRepository.save(batch);

        // Publish Domain Event
        PaymentDispatchedEvent event = new PaymentDispatchedEvent(
                tenantId,
                saved.getId(),
                saved.getBatchNumber(),
                saved.getLegalEntity().getId(),
                saved.getTotalAmount(),
                saved.getCurrency(),
                saved.getItemCount(),
                currentUser.getId(),
                eventPayloads,
                Instant.now()
        );
        eventPublisher.publishEvent(event);

        log.info("Payment Batch {} successfully approved and dispatched by {}", saved.getBatchNumber(), currentUser.getEmail());
        return PaymentBatchResponse.from(saved);
    }

    @Override
    @Transactional
    public PaymentBatchResponse cancelPaymentBatch(UUID id) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        PaymentBatch batch = paymentBatchRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new SpendSyncException("Payment Batch not found", HttpStatus.NOT_FOUND, "PAYMENT_BATCH_NOT_FOUND") {});

        if (batch.getStatus() == PaymentBatchStatus.DISPATCHED) {
            throw new SpendSyncException("Dispatched payment batches cannot be cancelled", HttpStatus.BAD_REQUEST, "CANNOT_CANCEL_DISPATCHED_BATCH") {};
        }

        batch.setStatus(PaymentBatchStatus.CANCELLED);
        PaymentBatch saved = paymentBatchRepository.save(batch);
        log.info("Payment Batch {} cancelled", saved.getBatchNumber());
        return PaymentBatchResponse.from(saved);
    }

    private String generateBatchNumber(UUID tenantId) {
        int currentYear = LocalDate.now().getYear();
        String prefix = String.format("PAY-%d-", currentYear);
        long count = paymentBatchRepository.countByTenantIdAndBatchNumberPrefix(tenantId, prefix);
        return String.format("PAY-%d-%05d", currentYear, count + 1);
    }

    private String generatePain001Xml(PaymentBatch batch, LegalEntity legalEntity) {
        return String.format("""
                <?xml version="1.0" encoding="UTF-8"?>
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pain.001.001.03">
                  <CstmrCdtTrfInitn>
                    <GrpHdr>
                      <MsgId>%s</MsgId>
                      <CreDtTm>%s</CreDtTm>
                      <NbOfTxs>%d</NbOfTxs>
                      <CtrlSum>%.2f</CtrlSum>
                      <InitgPty>
                        <Nm>%s</Nm>
                        <Id><OrgId><Othr><Id>%s</Id></Othr></OrgId></Id>
                      </InitgPty>
                    </GrpHdr>
                  </CstmrCdtTrfInitn>
                </Document>
                """,
                batch.getBatchNumber(),
                Instant.now().toString(),
                batch.getLineItems().size(),
                batch.getTotalAmount(),
                legalEntity.getName(),
                legalEntity.getTaxNumber()
        );
    }

    private User resolveCurrentUser(UUID tenantId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return userRepository.findByIdAndTenantId(principal.getId(), tenantId)
                    .orElseThrow(() -> new SpendSyncException("User not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND") {});
        }
        return userRepository.findAllByTenantId(tenantId).stream().findFirst()
                .orElseThrow(() -> new SpendSyncException("Default user not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND") {});
    }
}
