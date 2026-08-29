package com.enterprise.spendsync.receiving.service;

import com.enterprise.spendsync.core.internal.domain.Facility;
import com.enterprise.spendsync.core.internal.domain.FacilityType;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.repository.FacilityRepository;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.purchasing.internal.domain.Incoterms;
import com.enterprise.spendsync.purchasing.internal.domain.PaymentTerms;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrder;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderLineItem;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderStatus;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderLineItemRepository;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderRepository;
import com.enterprise.spendsync.receiving.internal.domain.GoodsReceipt;
import com.enterprise.spendsync.receiving.internal.dto.CreateGRLineItemRequest;
import com.enterprise.spendsync.receiving.internal.dto.CreateGoodsReceiptRequest;
import com.enterprise.spendsync.receiving.internal.dto.GoodsReceiptResponse;
import com.enterprise.spendsync.receiving.internal.dto.PendingPOForReceivingResponse;
import com.enterprise.spendsync.receiving.internal.event.GoodsReceivedEvent;
import com.enterprise.spendsync.receiving.internal.repository.GoodsReceiptLineItemRepository;
import com.enterprise.spendsync.receiving.internal.repository.GoodsReceiptRepository;
import com.enterprise.spendsync.receiving.internal.service.GoodsReceiptServiceImpl;
import com.enterprise.spendsync.requisition.internal.domain.PurchaseRequisition;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionStatus;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.notification.internal.service.EmailService;
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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoodsReceiptService Unit & Mock Tests (GRN, Over-Delivery Tolerance, Inspection & Status Sync)")
class GoodsReceiptServiceTest {

    @Mock
    private GoodsReceiptRepository goodsReceiptRepository;
    @Mock
    private GoodsReceiptLineItemRepository goodsReceiptLineItemRepository;
    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private PurchaseOrderLineItemRepository purchaseOrderLineItemRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private GoodsReceiptServiceImpl goodsReceiptService;

    private UUID tenantId;
    private Tenant tenant;
    private LegalEntity legalEntity;
    private Facility facility;
    private User receiver;
    private User requisitioner;
    private Vendor vendor;
    private PurchaseRequisition requisition;
    private PurchaseOrder issuedPo;
    private PurchaseOrderLineItem poLine;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("SpendSync Global");

        legalEntity = new LegalEntity(tenant, "SpendSync Turkey", "TR01", "1234567890", "TRY", "Istanbul", "TR");
        legalEntity.setId(UUID.randomUUID());

        facility = new Facility(tenant, legalEntity, "Main Warehouse", "FAC-01", FacilityType.WAREHOUSE, "Gebze OSB");
        facility.setId(UUID.randomUUID());

        receiver = new User("receiver@spendsync.com", "pass", "Warehouse", "Clerk", null, "TR");
        receiver.setId(UUID.randomUUID());

        requisitioner = new User("buyer@spendsync.com", "pass", "Jane", "Requisitioner", null, "TR");
        requisitioner.setId(UUID.randomUUID());

        vendor = new Vendor();
        vendor.setId(UUID.randomUUID());
        vendor.setName("Global Server Supplies");

        requisition = new PurchaseRequisition(
                tenant, "PR-2026-00001", requisitioner, legalEntity, null, facility,
                null, RequisitionStatus.APPROVED, new BigDecimal("100000.00"), "TRY", "Hardware", "Server fleet"
        );
        requisition.setId(UUID.randomUUID());

        issuedPo = new PurchaseOrder(
                tenant, "PO-2026-00001", requisition, legalEntity, null, facility,
                vendor, Incoterms.DAP, "TRY", PaymentTerms.NET_30, "Deliver promptly", receiver
        );
        issuedPo.setId(UUID.randomUUID());
        issuedPo.setStatus(PurchaseOrderStatus.ISSUED);

        poLine = new PurchaseOrderLineItem(
                tenant, issuedPo, null, 1, "Server Rack 42U", "IT_HARDWARE",
                new BigDecimal("10.0000"), "PIECE", new BigDecimal("10000.0000"),
                new BigDecimal("5.00"), BigDecimal.ZERO, LocalDate.now().plusDays(5) // 5% over-delivery tolerance
        );
        poLine.setId(UUID.randomUUID());
        issuedPo.addLineItem(poLine);

        UserPrincipal principal = new UserPrincipal(
                receiver.getId(), tenantId, null, "FACILITY_USER", receiver.getEmail(), null, "Warehouse Clerk", true, Set.of(), Set.of()
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
    @DisplayName("TC-06-01: Full delivery for ISSUED PO creates GRN and sets PO to FULFILLED")
    void shouldCreateFullDeliveryGoodsReceipt() {
        CreateGoodsReceiptRequest request = new CreateGoodsReceiptRequest(
                issuedPo.getId(),
                "IRS-2026-00100",
                LocalDate.now(),
                facility.getId(),
                "Complete order received in good condition",
                List.of(new CreateGRLineItemRequest(
                        poLine.getId(),
                        new BigDecimal("10.0000"),
                        new BigDecimal("10.0000"),
                        BigDecimal.ZERO,
                        null,
                        "Checked and verified serial numbers"
                ))
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findByIdAndTenantId(receiver.getId(), tenantId)).thenReturn(Optional.of(receiver));
        when(purchaseOrderRepository.findByIdAndTenantId(issuedPo.getId(), tenantId)).thenReturn(Optional.of(issuedPo));
        when(facilityRepository.findByIdAndTenantId(facility.getId(), tenantId)).thenReturn(Optional.of(facility));
        when(purchaseOrderLineItemRepository.findById(poLine.getId())).thenReturn(Optional.of(poLine));
        when(goodsReceiptLineItemRepository.sumAcceptedQuantityByPoLineId(poLine.getId())).thenReturn(BigDecimal.ZERO);
        when(goodsReceiptRepository.countByTenantIdAndReceiptNumberPrefix(eq(tenantId), any())).thenReturn(0L);

        when(goodsReceiptRepository.save(any(GoodsReceipt.class))).thenAnswer(i -> {
            GoodsReceipt gr = i.getArgument(0);
            gr.setId(UUID.randomUUID());
            return gr;
        });

        GoodsReceiptResponse response = goodsReceiptService.createGoodsReceipt(request);

        assertThat(response).isNotNull();
        assertThat(response.receiptNumber()).startsWith("GR-");
        assertThat(response.waybillNumber()).isEqualTo("IRS-2026-00100");
        assertThat(issuedPo.getStatus()).isEqualTo(PurchaseOrderStatus.FULFILLED);

        verify(eventPublisher).publishEvent(any(GoodsReceivedEvent.class));
        verify(emailService).sendTemplatedEmail(
                eq(requisitioner.getEmail()),
                contains("Goods Receipt Completed"),
                eq("goods-receipt-completed"),
                anyMap()
        );
    }

    @Test
    @DisplayName("TC-06-02: Partial delivery sets PO status to PARTIALLY_RECEIVED")
    void shouldHandlePartialDelivery() {
        CreateGoodsReceiptRequest request = new CreateGoodsReceiptRequest(
                issuedPo.getId(),
                "IRS-2026-00101",
                LocalDate.now(),
                facility.getId(),
                "First shipment of 6 units",
                List.of(new CreateGRLineItemRequest(
                        poLine.getId(),
                        new BigDecimal("6.0000"),
                        new BigDecimal("6.0000"),
                        BigDecimal.ZERO,
                        null,
                        null
                ))
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findByIdAndTenantId(receiver.getId(), tenantId)).thenReturn(Optional.of(receiver));
        when(purchaseOrderRepository.findByIdAndTenantId(issuedPo.getId(), tenantId)).thenReturn(Optional.of(issuedPo));
        when(facilityRepository.findByIdAndTenantId(facility.getId(), tenantId)).thenReturn(Optional.of(facility));
        when(purchaseOrderLineItemRepository.findById(poLine.getId())).thenReturn(Optional.of(poLine));
        when(goodsReceiptLineItemRepository.sumAcceptedQuantityByPoLineId(poLine.getId())).thenReturn(BigDecimal.ZERO);
        when(goodsReceiptRepository.countByTenantIdAndReceiptNumberPrefix(eq(tenantId), any())).thenReturn(0L);
        when(goodsReceiptRepository.save(any(GoodsReceipt.class))).thenAnswer(i -> {
            GoodsReceipt gr = i.getArgument(0);
            gr.setId(UUID.randomUUID());
            return gr;
        });

        GoodsReceiptResponse response = goodsReceiptService.createGoodsReceipt(request);

        assertThat(response).isNotNull();
        assertThat(issuedPo.getStatus()).isEqualTo(PurchaseOrderStatus.PARTIALLY_RECEIVED);
    }

    @Test
    @DisplayName("TC-06-03: Reject GRN creation for non-deliverable PO status (e.g. DRAFT)")
    void shouldRejectGrnForDraftPo() {
        issuedPo.setStatus(PurchaseOrderStatus.DRAFT);

        CreateGoodsReceiptRequest request = new CreateGoodsReceiptRequest(
                issuedPo.getId(), "IRS-01", LocalDate.now(), facility.getId(), null, List.of()
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findByIdAndTenantId(receiver.getId(), tenantId)).thenReturn(Optional.of(receiver));
        when(purchaseOrderRepository.findByIdAndTenantId(issuedPo.getId(), tenantId)).thenReturn(Optional.of(issuedPo));

        assertThatThrownBy(() -> goodsReceiptService.createGoodsReceipt(request))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException sex = (SpendSyncException) ex;
                    assertThat(sex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(sex.getErrorCode()).isEqualTo("INVALID_PO_STATUS_FOR_RECEIVING");
                });
    }

    @Test
    @DisplayName("TC-06-04: Reject GRN when receivedQuantity != acceptedQuantity + rejectedQuantity")
    void shouldRejectQuantityMismatch() {
        CreateGoodsReceiptRequest request = new CreateGoodsReceiptRequest(
                issuedPo.getId(),
                "IRS-02",
                LocalDate.now(),
                facility.getId(),
                null,
                List.of(new CreateGRLineItemRequest(
                        poLine.getId(),
                        new BigDecimal("10.0000"), // Received 10
                        new BigDecimal("7.0000"),  // Accepted 7
                        new BigDecimal("2.0000"),  // Rejected 2 -> Sum is 9, mismatch!
                        "Damaged",
                        null
                ))
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findByIdAndTenantId(receiver.getId(), tenantId)).thenReturn(Optional.of(receiver));
        when(purchaseOrderRepository.findByIdAndTenantId(issuedPo.getId(), tenantId)).thenReturn(Optional.of(issuedPo));
        when(facilityRepository.findByIdAndTenantId(facility.getId(), tenantId)).thenReturn(Optional.of(facility));
        when(purchaseOrderLineItemRepository.findById(poLine.getId())).thenReturn(Optional.of(poLine));

        assertThatThrownBy(() -> goodsReceiptService.createGoodsReceipt(request))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException sex = (SpendSyncException) ex;
                    assertThat(sex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(sex.getErrorCode()).isEqualTo("QUANTITY_INCONSISTENCY");
                });
    }

    @Test
    @DisplayName("TC-06-05: Reject GRN when rejectedQuantity > 0 but rejectionReason is blank")
    void shouldRequireRejectionReasonWhenRejectedQtyGreaterThanZero() {
        CreateGoodsReceiptRequest request = new CreateGoodsReceiptRequest(
                issuedPo.getId(),
                "IRS-03",
                LocalDate.now(),
                facility.getId(),
                null,
                List.of(new CreateGRLineItemRequest(
                        poLine.getId(),
                        new BigDecimal("10.0000"),
                        new BigDecimal("8.0000"),
                        new BigDecimal("2.0000"),
                        "", // Blank rejection reason!
                        null
                ))
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findByIdAndTenantId(receiver.getId(), tenantId)).thenReturn(Optional.of(receiver));
        when(purchaseOrderRepository.findByIdAndTenantId(issuedPo.getId(), tenantId)).thenReturn(Optional.of(issuedPo));
        when(facilityRepository.findByIdAndTenantId(facility.getId(), tenantId)).thenReturn(Optional.of(facility));
        when(purchaseOrderLineItemRepository.findById(poLine.getId())).thenReturn(Optional.of(poLine));

        assertThatThrownBy(() -> goodsReceiptService.createGoodsReceipt(request))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException sex = (SpendSyncException) ex;
                    assertThat(sex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(sex.getErrorCode()).isEqualTo("REJECTION_REASON_MANDATORY");
                });
    }

    @Test
    @DisplayName("TC-06-06: Reject GRN when accepted quantity exceeds over-delivery tolerance (5%)")
    void shouldRejectOverDeliveryToleranceExceeded() {
        // Ordered 10 units, 5% tolerance -> max 10.5 units
        CreateGoodsReceiptRequest request = new CreateGoodsReceiptRequest(
                issuedPo.getId(),
                "IRS-04",
                LocalDate.now(),
                facility.getId(),
                null,
                List.of(new CreateGRLineItemRequest(
                        poLine.getId(),
                        new BigDecimal("11.0000"), // 11 > 10.5
                        new BigDecimal("11.0000"),
                        BigDecimal.ZERO,
                        null,
                        null
                ))
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findByIdAndTenantId(receiver.getId(), tenantId)).thenReturn(Optional.of(receiver));
        when(purchaseOrderRepository.findByIdAndTenantId(issuedPo.getId(), tenantId)).thenReturn(Optional.of(issuedPo));
        when(facilityRepository.findByIdAndTenantId(facility.getId(), tenantId)).thenReturn(Optional.of(facility));
        when(purchaseOrderLineItemRepository.findById(poLine.getId())).thenReturn(Optional.of(poLine));
        when(goodsReceiptLineItemRepository.sumAcceptedQuantityByPoLineId(poLine.getId())).thenReturn(BigDecimal.ZERO);

        assertThatThrownBy(() -> goodsReceiptService.createGoodsReceipt(request))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException sex = (SpendSyncException) ex;
                    assertThat(sex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(sex.getErrorCode()).isEqualTo("OVER_DELIVERY_TOLERANCE_EXCEEDED");
                });
    }

    @Test
    @DisplayName("TC-06-07: Reject GRN if PO Line does not belong to specified PO")
    void shouldRejectMismatchedPoLineItem() {
        PurchaseOrder anotherPo = new PurchaseOrder();
        anotherPo.setId(UUID.randomUUID());

        PurchaseOrderLineItem foreignLine = new PurchaseOrderLineItem();
        foreignLine.setId(UUID.randomUUID());
        foreignLine.setPurchaseOrder(anotherPo);

        CreateGoodsReceiptRequest request = new CreateGoodsReceiptRequest(
                issuedPo.getId(),
                "IRS-05",
                LocalDate.now(),
                facility.getId(),
                null,
                List.of(new CreateGRLineItemRequest(
                        foreignLine.getId(),
                        new BigDecimal("1.0000"),
                        new BigDecimal("1.0000"),
                        BigDecimal.ZERO,
                        null,
                        null
                ))
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findByIdAndTenantId(receiver.getId(), tenantId)).thenReturn(Optional.of(receiver));
        when(purchaseOrderRepository.findByIdAndTenantId(issuedPo.getId(), tenantId)).thenReturn(Optional.of(issuedPo));
        when(facilityRepository.findByIdAndTenantId(facility.getId(), tenantId)).thenReturn(Optional.of(facility));
        when(purchaseOrderLineItemRepository.findById(foreignLine.getId())).thenReturn(Optional.of(foreignLine));

        assertThatThrownBy(() -> goodsReceiptService.createGoodsReceipt(request))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException sex = (SpendSyncException) ex;
                    assertThat(sex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(sex.getErrorCode()).isEqualTo("INVALID_PO_LINE_MAPPING");
                });
    }

    @Test
    @DisplayName("TC-06-08: Retrieve Pending POs for receiving filtering only ISSUED and PARTIALLY_RECEIVED")
    void shouldGetPendingOrdersForReceiving() {
        PurchaseOrder draftPo = new PurchaseOrder();
        draftPo.setId(UUID.randomUUID());
        draftPo.setStatus(PurchaseOrderStatus.DRAFT);

        when(purchaseOrderRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId))
                .thenReturn(List.of(issuedPo, draftPo));

        List<PendingPOForReceivingResponse> pending = goodsReceiptService.getPendingOrdersForReceiving();

        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).poNumber()).isEqualTo("PO-2026-00001");
    }
}
