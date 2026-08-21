package com.enterprise.spendsync.payment.service;

import com.enterprise.spendsync.core.internal.domain.*;
import com.enterprise.spendsync.core.internal.repository.LegalEntityRepository;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.matching.internal.domain.*;
import com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceRepository;
import com.enterprise.spendsync.payment.internal.domain.*;
import com.enterprise.spendsync.payment.internal.dto.*;
import com.enterprise.spendsync.payment.internal.event.PaymentDispatchedEvent;
import com.enterprise.spendsync.payment.internal.repository.PaymentBatchItemRepository;
import com.enterprise.spendsync.payment.internal.repository.PaymentBatchRepository;
import com.enterprise.spendsync.payment.internal.service.PaymentServiceImpl;
import com.enterprise.spendsync.purchasing.internal.domain.*;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.notification.EmailService;
import com.enterprise.spendsync.shared.security.UserPrincipal;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit & Mock Tests (Payment Batch, SoD & Remittance)")
class PaymentServiceTest {

    @Mock
    private PaymentBatchRepository paymentBatchRepository;
    @Mock
    private PaymentBatchItemRepository paymentBatchItemRepository;
    @Mock
    private SupplierInvoiceRepository supplierInvoiceRepository;
    @Mock
    private LegalEntityRepository legalEntityRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UUID tenantId;
    private Tenant tenant;
    private LegalEntity legalEntity;
    private Vendor vendor;
    private User apSpecialist;
    private User cfo;
    private SupplierInvoice invoice1;
    private SupplierInvoice invoice2;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("SpendSync Global");

        legalEntity = new LegalEntity(tenant, "SpendSync Turkey", "TR01", "1234567890", "TRY", "Istanbul", "TR");
        legalEntity.setId(UUID.randomUUID());

        vendor = new Vendor(
                tenant, "Global IT Hardware Inc.", "9998887776", "Maslak",
                VendorCategory.IT_HARDWARE, VendorTier.TIER_1_STRATEGIC, true,
                "finance@globalit.com", "+90 212 999 0000", "Maslak", "Istanbul", "TR",
                PaymentTerms.NET_30, "Garanti BBVA", "TR330006200000012345678901"
        );
        vendor.setId(UUID.randomUUID());

        apSpecialist = new User("ap@spendsync.com", "pass", "AP", "Specialist", null, "TR");
        apSpecialist.setId(UUID.randomUUID());
        apSpecialist.setRoles(Set.of(RoleType.AP_SPECIALIST));

        cfo = new User("cfo@spendsync.com", "pass", "Chief", "Financial Officer", null, "TR");
        cfo.setId(UUID.randomUUID());
        cfo.setRoles(Set.of(RoleType.APPROVER));

        invoice1 = new SupplierInvoice(
                tenant, "INV-2026-001", "ettn-001", LocalDate.now(), InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA,
                null, vendor, legalEntity, null, "TRY",
                new BigDecimal("100000.00"), new BigDecimal("20000.00"), new BigDecimal("120000.00")
        );
        invoice1.setId(UUID.randomUUID());
        invoice1.setStatus(InvoiceStatus.APPROVED_FOR_PAYMENT);

        invoice2 = new SupplierInvoice(
                tenant, "INV-2026-002", "ettn-002", LocalDate.now(), InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA,
                null, vendor, legalEntity, null, "TRY",
                new BigDecimal("50000.00"), new BigDecimal("10000.00"), new BigDecimal("60000.00")
        );
        invoice2.setId(UUID.randomUUID());
        invoice2.setStatus(InvoiceStatus.APPROVED_FOR_PAYMENT);

        UserPrincipal principal = new UserPrincipal(
                apSpecialist.getId(), tenantId, null, "AP_SPECIALIST", apSpecialist.getEmail(), null, "AP Specialist", true, Set.of(), Set.of()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, "token", Set.of()));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("TC-08-01: Creates Payment Batch aggregating approved invoices and calculates total amount")
    void shouldCreatePaymentBatchSuccessfully() {
        CreatePaymentBatchRequest request = new CreatePaymentBatchRequest(
                legalEntity.getId(),
                PaymentMethod.ISO_20022_PAIN_001,
                "idemp-batch-001",
                List.of(invoice1.getId(), invoice2.getId())
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findByIdAndTenantId(apSpecialist.getId(), tenantId)).thenReturn(Optional.of(apSpecialist));
        when(paymentBatchRepository.findByTenantIdAndIdempotencyKey(tenantId, "idemp-batch-001")).thenReturn(Optional.empty());
        when(legalEntityRepository.findByIdAndTenantId(legalEntity.getId(), tenantId)).thenReturn(Optional.of(legalEntity));
        when(paymentBatchRepository.countByTenantIdAndBatchNumberPrefix(eq(tenantId), any())).thenReturn(0L);

        when(supplierInvoiceRepository.findByIdAndTenantId(invoice1.getId(), tenantId)).thenReturn(Optional.of(invoice1));
        when(supplierInvoiceRepository.findByIdAndTenantId(invoice2.getId(), tenantId)).thenReturn(Optional.of(invoice2));
        when(paymentBatchItemRepository.isInvoiceAlreadyInActiveBatch(any())).thenReturn(false);

        when(paymentBatchRepository.save(any(PaymentBatch.class))).thenAnswer(i -> {
            PaymentBatch pb = i.getArgument(0);
            pb.setId(UUID.randomUUID());
            return pb;
        });

        PaymentBatchResponse response = paymentService.createPaymentBatch(request);

        assertThat(response).isNotNull();
        assertThat(response.itemCount()).isEqualTo(2);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("180000.00"));
        assertThat(response.status()).isEqualTo(PaymentBatchStatus.DRAFT);
        assertThat(response.batchNumber()).startsWith("PAY-");
        assertThat(response.xmlPayload()).contains("urn:iso:std:iso:20022:tech:xsd:pain.001.001.03");
    }

    @Test
    @DisplayName("TC-08-02: Rejects non-approved invoices from inclusion into Payment Batch")
    void shouldRejectNonApprovedInvoices() {
        invoice1.setStatus(InvoiceStatus.SUBMITTED); // not yet approved

        CreatePaymentBatchRequest request = new CreatePaymentBatchRequest(
                legalEntity.getId(),
                PaymentMethod.ISO_20022_PAIN_001,
                "idemp-batch-002",
                List.of(invoice1.getId())
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findByIdAndTenantId(apSpecialist.getId(), tenantId)).thenReturn(Optional.of(apSpecialist));
        when(paymentBatchRepository.findByTenantIdAndIdempotencyKey(tenantId, "idemp-batch-002")).thenReturn(Optional.empty());
        when(legalEntityRepository.findByIdAndTenantId(legalEntity.getId(), tenantId)).thenReturn(Optional.of(legalEntity));
        when(supplierInvoiceRepository.findByIdAndTenantId(invoice1.getId(), tenantId)).thenReturn(Optional.of(invoice1));

        assertThatThrownBy(() -> paymentService.createPaymentBatch(request))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException sex = (SpendSyncException) ex;
                    assertThat(sex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(sex.getErrorCode()).isEqualTo("INVOICE_NOT_APPROVED");
                });
    }

    @Test
    @DisplayName("TC-08-03: Double-spending prevention: rejects invoice already present in active batch")
    void shouldPreventDoubleSpendingAttempt() {
        CreatePaymentBatchRequest request = new CreatePaymentBatchRequest(
                legalEntity.getId(),
                PaymentMethod.ISO_20022_PAIN_001,
                "idemp-batch-003",
                List.of(invoice1.getId())
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findByIdAndTenantId(apSpecialist.getId(), tenantId)).thenReturn(Optional.of(apSpecialist));
        when(paymentBatchRepository.findByTenantIdAndIdempotencyKey(tenantId, "idemp-batch-003")).thenReturn(Optional.empty());
        when(legalEntityRepository.findByIdAndTenantId(legalEntity.getId(), tenantId)).thenReturn(Optional.of(legalEntity));
        when(supplierInvoiceRepository.findByIdAndTenantId(invoice1.getId(), tenantId)).thenReturn(Optional.of(invoice1));
        when(paymentBatchItemRepository.isInvoiceAlreadyInActiveBatch(invoice1.getId())).thenReturn(true);

        assertThatThrownBy(() -> paymentService.createPaymentBatch(request))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException sex = (SpendSyncException) ex;
                    assertThat(sex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(sex.getErrorCode()).isEqualTo("DOUBLE_SPENDING_ATTEMPT_BLOCKED");
                });
    }

    @Test
    @DisplayName("TC-08-07: SoD Four-Eyes Principle: creator cannot approve their own payment batch")
    void shouldEnforceFourEyesPrincipleOnApproval() {
        PaymentBatch batch = new PaymentBatch(
                tenant, "PAY-2026-00001", legalEntity, PaymentMethod.ISO_20022_PAIN_001,
                new BigDecimal("120000.00"), "TRY", apSpecialist, "idemp-001"
        );
        batch.setId(UUID.randomUUID());

        ApprovePaymentBatchRequest request = new ApprovePaymentBatchRequest("Attempt self approval");

        when(userRepository.findByIdAndTenantId(apSpecialist.getId(), tenantId)).thenReturn(Optional.of(apSpecialist));
        when(paymentBatchRepository.findByIdAndTenantId(batch.getId(), tenantId)).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> paymentService.approveAndDispatchPaymentBatch(batch.getId(), request))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException sex = (SpendSyncException) ex;
                    assertThat(sex.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(sex.getErrorCode()).isEqualTo("SOD_VIOLATION_SELF_APPROVAL");
                });
    }

    @Test
    @DisplayName("TC-08-08: CFO approves and dispatches batch: flips invoices to PAID, emails remittance and publishes event")
    void shouldApproveAndDispatchPaymentBatchByCfo() {
        // Authenticate as CFO
        UserPrincipal cfoPrincipal = new UserPrincipal(
                cfo.getId(), tenantId, null, "FINANCE_DIRECTOR", cfo.getEmail(), null, "CFO", true, Set.of(), Set.of()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(cfoPrincipal, "token", Set.of()));
        SecurityContextHolder.setContext(context);

        PaymentBatch batch = new PaymentBatch(
                tenant, "PAY-2026-00001", legalEntity, PaymentMethod.ISO_20022_PAIN_001,
                new BigDecimal("120000.00"), "TRY", apSpecialist, "idemp-001"
        );
        batch.setId(UUID.randomUUID());

        PaymentBatchItem item = new PaymentBatchItem(
                tenant, invoice1, vendor, "Global IT Hardware Inc.", "TR330006200000012345678901",
                new BigDecimal("120000.00"), BigDecimal.ZERO, new BigDecimal("120000.00")
        );
        batch.addLineItem(item);

        ApprovePaymentBatchRequest request = new ApprovePaymentBatchRequest("Approved by CFO for Swift EFT");

        when(userRepository.findByIdAndTenantId(cfo.getId(), tenantId)).thenReturn(Optional.of(cfo));
        when(paymentBatchRepository.findByIdAndTenantId(batch.getId(), tenantId)).thenReturn(Optional.of(batch));
        when(paymentBatchRepository.save(any(PaymentBatch.class))).thenAnswer(i -> i.getArgument(0));

        PaymentBatchResponse response = paymentService.approveAndDispatchPaymentBatch(batch.getId(), request);

        assertThat(response.status()).isEqualTo(PaymentBatchStatus.DISPATCHED);
        assertThat(invoice1.getStatus()).isEqualTo(InvoiceStatus.PAID);

        verify(emailService).sendTemplatedEmail(
                eq("finance@globalit.com"),
                contains("Payment Remittance Advice"),
                eq("payment-remittance-advice"),
                any()
        );

        verify(eventPublisher).publishEvent(any(PaymentDispatchedEvent.class));
    }

    @Test
    @DisplayName("TC-08-09: Cancelling a DRAFT payment batch transitions status to CANCELLED")
    void shouldCancelDraftPaymentBatch() {
        PaymentBatch batch = new PaymentBatch(
                tenant, "PAY-2026-00001", legalEntity, PaymentMethod.ISO_20022_PAIN_001,
                new BigDecimal("120000.00"), "TRY", apSpecialist, "idemp-001"
        );
        batch.setId(UUID.randomUUID());

        when(paymentBatchRepository.findByIdAndTenantId(batch.getId(), tenantId)).thenReturn(Optional.of(batch));
        when(paymentBatchRepository.save(any(PaymentBatch.class))).thenAnswer(i -> i.getArgument(0));

        PaymentBatchResponse response = paymentService.cancelPaymentBatch(batch.getId());

        assertThat(response.status()).isEqualTo(PaymentBatchStatus.CANCELLED);
    }

    @Test
    @DisplayName("TC-08-10: Due invoices retrieval filters only APPROVED_FOR_PAYMENT invoices")
    void shouldGetDueInvoices() {
        when(supplierInvoiceRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId))
                .thenReturn(List.of(invoice1, invoice2));
        when(paymentBatchItemRepository.isInvoiceAlreadyInActiveBatch(any())).thenReturn(false);

        List<DueInvoiceResponse> dueInvoices = paymentService.getDueInvoices();

        assertThat(dueInvoices).hasSize(2);
        assertThat(dueInvoices.get(0).invoiceNumber()).isEqualTo("INV-2026-001");
    }
}
