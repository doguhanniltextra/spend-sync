package com.enterprise.spendsync.requisition.domain;

import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.Facility;
import com.enterprise.spendsync.core.internal.domain.FacilityType;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.requisition.internal.domain.ApprovalStepStatus;
import com.enterprise.spendsync.requisition.internal.domain.PurchaseRequisition;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionApprovalStep;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionLineItem;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PurchaseRequisition Domain Entity Pure Unit Tests")
class PurchaseRequisitionTest {

    private Tenant tenant;
    private LegalEntity legalEntity;
    private CostCenter costCenter;
    private Facility facility;
    private User requisitioner;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName("SpendSync Global");

        legalEntity = new LegalEntity(tenant, "SpendSync Turkey", "TR01", "1234567890", "TRY", "Istanbul", "TR");
        legalEntity.setId(UUID.randomUUID());

        costCenter = new CostCenter(tenant, legalEntity, "CC-IT", "Information Technology");
        costCenter.setId(UUID.randomUUID());

        facility = new Facility(tenant, legalEntity, "Main Warehouse", "WH-01", FacilityType.WAREHOUSE, "Gebze OSB");
        facility.setId(UUID.randomUUID());

        requisitioner = new User("user@spendsync.com", "pass", "Ali", "Demir", null, "TR");
        requisitioner.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should recalculate totalAmount dynamically as line items are added and removed")
    void shouldRecalculateTotalAmountAccurately() {
        PurchaseRequisition pr = new PurchaseRequisition(
                tenant,
                "PR-2026-00001",
                requisitioner,
                legalEntity,
                costCenter,
                facility,
                null,
                RequisitionStatus.DRAFT,
                BigDecimal.ZERO,
                "TRY",
                "IT Equipment Request",
                "New developer workstations"
        );

        assertThat(pr.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        RequisitionLineItem item1 = new RequisitionLineItem(
                pr, tenant, 1, "MacBook Pro M3", "HARDWARE",
                new BigDecimal("2.0"), "PCS", new BigDecimal("75000.00"), LocalDate.now().plusDays(7)
        );
        pr.addLineItem(item1);

        // 2 * 75,000 = 150,000
        assertThat(pr.getTotalAmount()).isEqualByComparingTo(new BigDecimal("150000.00"));

        RequisitionLineItem item2 = new RequisitionLineItem(
                pr, tenant, 2, "Dell 4K Monitor", "HARDWARE",
                new BigDecimal("4.0"), "PCS", new BigDecimal("12500.00"), LocalDate.now().plusDays(7)
        );
        pr.addLineItem(item2);

        // 150,000 + (4 * 12,500) = 200,000
        assertThat(pr.getTotalAmount()).isEqualByComparingTo(new BigDecimal("200000.00"));
        assertThat(pr.getLineItems()).hasSize(2);
    }

    @Test
    @DisplayName("Should maintain approval steps collection correctly")
    void shouldManageApprovalSteps() {
        PurchaseRequisition pr = new PurchaseRequisition(
                tenant, "PR-2026-00002", requisitioner, legalEntity, costCenter, facility,
                null, RequisitionStatus.DRAFT, BigDecimal.ZERO, "TRY", "Title", "Justification"
        );

        User manager = new User("mgr@spendsync.com", "pass", "Jane", "Doe", null, "TR");
        manager.setId(UUID.randomUUID());

        RequisitionApprovalStep step1 = new RequisitionApprovalStep(pr, tenant, 1, manager, 1, ApprovalStepStatus.PENDING);
        pr.addApprovalStep(step1);

        assertThat(pr.getApprovalSteps()).hasSize(1);
        assertThat(pr.getApprovalSteps().get(0).getApprover().getEmail()).isEqualTo("mgr@spendsync.com");
    }
}
