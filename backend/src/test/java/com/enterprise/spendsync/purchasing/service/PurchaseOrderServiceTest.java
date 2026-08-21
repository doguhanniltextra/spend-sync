package com.enterprise.spendsync.purchasing.service;

import com.enterprise.spendsync.budget.internal.domain.BudgetPool;
import com.enterprise.spendsync.budget.internal.service.BudgetService;
import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.Facility;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.repository.*;
import com.enterprise.spendsync.purchasing.internal.domain.*;
import com.enterprise.spendsync.purchasing.internal.dto.*;
import com.enterprise.spendsync.purchasing.internal.event.PurchaseOrderCancelledEvent;
import com.enterprise.spendsync.purchasing.internal.event.PurchaseOrderIssuedEvent;
import com.enterprise.spendsync.purchasing.internal.event.PurchaseOrderRevisedEvent;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderLineItemRepository;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderRepository;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderRevisionRepository;
import com.enterprise.spendsync.purchasing.internal.repository.VendorRepository;
import com.enterprise.spendsync.purchasing.internal.service.PurchaseOrderServiceImpl;
import com.enterprise.spendsync.requisition.internal.domain.PurchaseRequisition;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionLineItem;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionStatus;
import com.enterprise.spendsync.requisition.internal.repository.PurchaseRequisitionRepository;
import com.enterprise.spendsync.shared.domain.CrossAssignmentDetector;
import com.enterprise.spendsync.shared.domain.CrossAssignmentWarning;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseOrderService Unit & Mock Tests (Lifecycle, DoA, Revision & Budget Sync)")
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private PurchaseOrderLineItemRepository lineItemRepository;
    @Mock
    private PurchaseOrderRevisionRepository revisionRepository;
    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private PurchaseRequisitionRepository requisitionRepository;
    @Mock
    private LegalEntityRepository legalEntityRepository;
    @Mock
    private CostCenterRepository costCenterRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BudgetService budgetService;
    @Mock
    private CrossAssignmentDetector crossAssignmentDetector;
    @Mock
    private EmailService emailService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PurchaseOrderServiceImpl purchaseOrderService;

    private UUID tenantId;
    private Tenant tenant;
    private LegalEntity legalEntity;
    private CostCenter costCenter;
    private Facility facility;
    private Vendor vendor;
    private User buyerUser;
    private PurchaseRequisition approvedRequisition;
    private BudgetPool budgetPool;
    private PurchaseOrder draftPo;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("SpendSync Global");

        legalEntity = new LegalEntity(tenant, "SpendSync Turkey", "TR01", "1112223334", "TRY", "Istanbul", "TR");
        legalEntity.setId(UUID.randomUUID());

        costCenter = new CostCenter(tenant, legalEntity, "CC-ENG", "Engineering");
        costCenter.setId(UUID.randomUUID());

        facility = new Facility(tenant, legalEntity, "Headquarters Dock", "FAC-HQ", com.enterprise.spendsync.core.internal.domain.FacilityType.WAREHOUSE, "Buyukdere Cad. No: 100 Maslak Istanbul");
        facility.setId(UUID.randomUUID());

        vendor = new Vendor(
                tenant, "Global Hardware Inc.", "9998887776", "Maslak",
                VendorCategory.IT_HARDWARE, VendorTier.TIER_1_STRATEGIC, true,
                "orders@globalhardware.com", "+90 212 111 2233", "Address", "Istanbul", "TR",
                PaymentTerms.NET_30, "Garanti BBVA", "TR1122334455"
        );
        vendor.setId(UUID.randomUUID());
        vendor.setStatus(VendorStatus.ACTIVE);

        buyerUser = new User("buyer@spendsync.com", "pass", "Buyer", "Officer", null, "TR");
        buyerUser.setId(UUID.randomUUID());

        budgetPool = new BudgetPool(
                tenant, legalEntity, costCenter, 2026, com.enterprise.spendsync.budget.internal.domain.BudgetPeriodType.ANNUAL,
                "2026", com.enterprise.spendsync.budget.internal.domain.BudgetStatus.ACTIVE,
                com.enterprise.spendsync.budget.internal.domain.BudgetEnforcementMode.HARD_STOP,
                BigDecimal.ZERO, new BigDecimal("1000000.00"), "TRY"
        );
        budgetPool.setId(UUID.randomUUID());

        approvedRequisition = new PurchaseRequisition(
                tenant, "PR-2026-00001", buyerUser, legalEntity, costCenter, facility,
                budgetPool, RequisitionStatus.APPROVED, new BigDecimal("500000.0000"),
                "TRY", "MacBook Pro fleet for Engineering", "Hardware refresh"
        );
        approvedRequisition.setId(UUID.randomUUID());

        RequisitionLineItem reqItem = new RequisitionLineItem(
                approvedRequisition, tenant, 1, "MacBook Pro 16", "Hardware",
                new BigDecimal("5.0000"), "PIECE", new BigDecimal("100000.0000"),
                LocalDate.now().plusDays(10)
        );
        reqItem.setId(UUID.randomUUID());
        approvedRequisition.addLineItem(reqItem);

        draftPo = new PurchaseOrder(
                tenant, "PO-2026-00001", approvedRequisition, legalEntity, costCenter, facility,
                vendor, Incoterms.DAP, "TRY", PaymentTerms.NET_30, "Deliver by end of month", buyerUser
        );
        draftPo.setId(UUID.randomUUID());

        PurchaseOrderLineItem poItem = new PurchaseOrderLineItem(
                tenant, draftPo, reqItem, 1, "MacBook Pro 16", "Hardware",
                new BigDecimal("5.0000"), "PIECE", new BigDecimal("100000.0000"),
                BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now().plusDays(10)
        );
        poItem.setId(UUID.randomUUID());
        draftPo.addLineItem(poItem);

        UserPrincipal principal = new UserPrincipal(
                buyerUser.getId(), tenantId, null, "BUYER", buyerUser.getEmail(), null, "Buyer Officer", true, Set.of(), Set.of()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, "token", Set.of()));
        SecurityContextHolder.setContext(context);

        lenient().when(crossAssignmentDetector.detect(any(), any())).thenReturn(CrossAssignmentWarning.none());
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should create Purchase Order from APPROVED Requisition with sequential PO number")
    void shouldCreatePoFromApprovedRequisition() {
        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest(
                approvedRequisition.getId(),
                legalEntity.getId(),
                costCenter.getId(),
                facility.getId(),
                vendor.getId(),
                Incoterms.DAP,
                PaymentTerms.NET_30,
                "TRY",
                "Ship to HQ",
                List.of(new POLineItemRequest(
                        approvedRequisition.getLineItems().get(0).getId(),
                        "MacBook Pro 16",
                        "Hardware",
                        new BigDecimal("5.0000"),
                        "PIECE",
                        new BigDecimal("100000.0000"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        LocalDate.now().plusDays(10)
                ))
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findByIdAndTenantId(buyerUser.getId(), tenantId)).thenReturn(Optional.of(buyerUser));
        when(legalEntityRepository.findByIdAndTenantId(legalEntity.getId(), tenantId)).thenReturn(Optional.of(legalEntity));
        when(costCenterRepository.findByIdAndTenantId(costCenter.getId(), tenantId)).thenReturn(Optional.of(costCenter));
        when(facilityRepository.findByIdAndTenantId(facility.getId(), tenantId)).thenReturn(Optional.of(facility));
        when(vendorRepository.findByIdAndTenantId(vendor.getId(), tenantId)).thenReturn(Optional.of(vendor));
        when(requisitionRepository.findByIdAndTenantId(approvedRequisition.getId(), tenantId)).thenReturn(Optional.of(approvedRequisition));
        when(purchaseOrderRepository.countByTenantId(tenantId)).thenReturn(0L);

        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(i -> {
            PurchaseOrder po = i.getArgument(0);
            po.setId(UUID.randomUUID());
            return po;
        });

        PurchaseOrderDetailResponse response = purchaseOrderService.createPurchaseOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.poNumber()).startsWith("PO-");
        assertThat(response.status()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("500000.0000"));
        assertThat(response.vendorName()).isEqualTo("Global Hardware Inc.");

        verify(purchaseOrderRepository).save(any(PurchaseOrder.class));
    }

    @Test
    @DisplayName("Should reject PO creation if Vendor is BLOCKED or INACTIVE (VENDOR_NOT_ACTIVE)")
    void shouldRejectPoIfVendorIsBlocked() {
        vendor.setStatus(VendorStatus.BLOCKED);

        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest(
                null, legalEntity.getId(), costCenter.getId(), facility.getId(), vendor.getId(),
                Incoterms.DAP, PaymentTerms.NET_30, "TRY", null, List.of()
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findByIdAndTenantId(buyerUser.getId(), tenantId)).thenReturn(Optional.of(buyerUser));
        when(legalEntityRepository.findByIdAndTenantId(legalEntity.getId(), tenantId)).thenReturn(Optional.of(legalEntity));
        when(costCenterRepository.findByIdAndTenantId(costCenter.getId(), tenantId)).thenReturn(Optional.of(costCenter));
        when(facilityRepository.findByIdAndTenantId(facility.getId(), tenantId)).thenReturn(Optional.of(facility));
        when(vendorRepository.findByIdAndTenantId(vendor.getId(), tenantId)).thenReturn(Optional.of(vendor));

        assertThatThrownBy(() -> purchaseOrderService.createPurchaseOrder(request))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException sex = (SpendSyncException) ex;
                    assertThat(sex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(sex.getErrorCode()).isEqualTo("VENDOR_NOT_ACTIVE");
                });
    }

    @Test
    @DisplayName("Should reject PO creation if linked Requisition is not in APPROVED status")
    void shouldRejectPoIfRequisitionNotApproved() {
        approvedRequisition.setStatus(RequisitionStatus.PENDING_APPROVAL);

        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest(
                approvedRequisition.getId(), legalEntity.getId(), costCenter.getId(), facility.getId(), vendor.getId(),
                Incoterms.DAP, PaymentTerms.NET_30, "TRY", null, List.of()
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findByIdAndTenantId(buyerUser.getId(), tenantId)).thenReturn(Optional.of(buyerUser));
        when(legalEntityRepository.findByIdAndTenantId(legalEntity.getId(), tenantId)).thenReturn(Optional.of(legalEntity));
        when(costCenterRepository.findByIdAndTenantId(costCenter.getId(), tenantId)).thenReturn(Optional.of(costCenter));
        when(facilityRepository.findByIdAndTenantId(facility.getId(), tenantId)).thenReturn(Optional.of(facility));
        when(vendorRepository.findByIdAndTenantId(vendor.getId(), tenantId)).thenReturn(Optional.of(vendor));
        when(requisitionRepository.findByIdAndTenantId(approvedRequisition.getId(), tenantId)).thenReturn(Optional.of(approvedRequisition));

        assertThatThrownBy(() -> purchaseOrderService.createPurchaseOrder(request))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException sex = (SpendSyncException) ex;
                    assertThat(sex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(sex.getErrorCode()).isEqualTo("REQUISITION_NOT_APPROVED");
                });
    }

    @Test
    @DisplayName("Should issue Purchase Order, send vendor email notification, and publish PurchaseOrderIssuedEvent")
    void shouldIssuePurchaseOrder() {
        when(purchaseOrderRepository.findByIdAndTenantId(draftPo.getId(), tenantId)).thenReturn(Optional.of(draftPo));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(i -> i.getArgument(0));

        PurchaseOrderDetailResponse response = purchaseOrderService.issuePurchaseOrder(draftPo.getId());

        assertThat(response.status()).isEqualTo(PurchaseOrderStatus.ISSUED);
        assertThat(draftPo.getStatus()).isEqualTo(PurchaseOrderStatus.ISSUED);
        assertThat(draftPo.getIssuedAt()).isNotNull();

        verify(emailService).sendTemplatedEmail(
                eq("orders@globalhardware.com"),
                contains("PO-2026-00001"),
                eq("purchase-order-issued"),
                anyMap()
        );

        verify(eventPublisher).publishEvent(any(PurchaseOrderIssuedEvent.class));
    }

    @Test
    @DisplayName("Should revise Purchase Order with budget increase differential and record snapshot")
    void shouldRevisePurchaseOrderWithBudgetIncrease() {
        draftPo.setStatus(PurchaseOrderStatus.ISSUED);
        UUID lineItemId = draftPo.getLineItems().get(0).getId();

        RevisePurchaseOrderRequest reviseRequest = new RevisePurchaseOrderRequest(
                "Scope expansion - added 2 more laptops",
                List.of(new RevisePOLineItemRequest(
                        lineItemId,
                        "MacBook Pro 16",
                        "Hardware",
                        new BigDecimal("7.0000"), // Increased from 5 to 7
                        "PIECE",
                        new BigDecimal("100000.0000"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        LocalDate.now().plusDays(15)
                ))
        );

        when(userRepository.findByIdAndTenantId(buyerUser.getId(), tenantId)).thenReturn(Optional.of(buyerUser));
        when(purchaseOrderRepository.findByIdAndTenantId(draftPo.getId(), tenantId)).thenReturn(Optional.of(draftPo));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(i -> i.getArgument(0));

        PurchaseOrderDetailResponse response = purchaseOrderService.revisePurchaseOrder(draftPo.getId(), reviseRequest);

        assertThat(draftPo.getRevisionNumber()).isEqualTo(1);
        assertThat(draftPo.getTotalAmount()).isEqualByComparingTo(new BigDecimal("700000.0000"));

        // Differential of 200,000 should be reserved in budget
        verify(budgetService).reserveBudget(
                eq(budgetPool.getId()),
                argThat(amt -> amt.compareTo(new BigDecimal("200000.0000")) == 0),
                eq(draftPo.getId()),
                eq("PURCHASE_ORDER_REVISION"),
                contains("PO Revision Increase")
        );

        verify(revisionRepository).save(any(PurchaseOrderRevision.class));
        verify(eventPublisher).publishEvent(any(PurchaseOrderRevisedEvent.class));
    }

    @Test
    @DisplayName("Should cancel Purchase Order, release reserved budget, and publish PurchaseOrderCancelledEvent")
    void shouldCancelPurchaseOrderAndReleaseBudget() {
        draftPo.setStatus(PurchaseOrderStatus.ISSUED);

        CancelPurchaseOrderRequest cancelRequest = new CancelPurchaseOrderRequest("Project postponed to next fiscal quarter");

        when(userRepository.findByIdAndTenantId(buyerUser.getId(), tenantId)).thenReturn(Optional.of(buyerUser));
        when(purchaseOrderRepository.findByIdAndTenantId(draftPo.getId(), tenantId)).thenReturn(Optional.of(draftPo));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(i -> i.getArgument(0));

        PurchaseOrderDetailResponse response = purchaseOrderService.cancelPurchaseOrder(draftPo.getId(), cancelRequest);

        assertThat(draftPo.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELLED);

        // Budget of 500,000 released
        verify(budgetService).releaseBudget(
                eq(budgetPool.getId()),
                argThat(amt -> amt.compareTo(new BigDecimal("500000.0000")) == 0),
                eq(draftPo.getId()),
                eq("PURCHASE_ORDER"),
                contains("PO Cancelled")
        );

        verify(eventPublisher).publishEvent(any(PurchaseOrderCancelledEvent.class));
    }
}
