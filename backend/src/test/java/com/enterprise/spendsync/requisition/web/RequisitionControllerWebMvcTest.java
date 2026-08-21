package com.enterprise.spendsync.requisition.web;

import com.enterprise.spendsync.requisition.internal.domain.ApprovalStepStatus;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionStatus;
import com.enterprise.spendsync.requisition.internal.dto.*;
import com.enterprise.spendsync.requisition.internal.service.RequisitionService;
import com.enterprise.spendsync.requisition.internal.web.RequisitionController;
import com.enterprise.spendsync.shared.config.Endpoints;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequisitionController REST Web API Slice Tests")
class RequisitionControllerWebMvcTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private RequisitionService requisitionService;

    @InjectMocks
    private RequisitionController requisitionController;

    private UUID prId;
    private RequisitionDetailResponse sampleDetailResponse;
    private RequisitionSummaryResponse sampleSummaryResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(requisitionController).build();
        prId = UUID.randomUUID();

        sampleDetailResponse = new RequisitionDetailResponse(
                prId,
                "PR-2026-00001",
                UUID.randomUUID(),
                "Ali Demir",
                "ali.demir@spendsync.com",
                UUID.randomUUID(),
                "SpendSync Turkey",
                UUID.randomUUID(),
                "Engineering",
                "CC-ENG",
                UUID.randomUUID(),
                "Main Warehouse",
                UUID.randomUUID(),
                RequisitionStatus.PENDING_APPROVAL,
                new BigDecimal("50000.00"),
                "TRY",
                "Developer Workstations",
                "Hardware refresh",
                null,
                null,
                List.of(new LineItemResponse(UUID.randomUUID(), 1, "MacBook", "HARDWARE", new BigDecimal("1.0"), "PCS", new BigDecimal("50000.00"), new BigDecimal("50000.00"), LocalDate.now())),
                List.of(new ApprovalStepResponse(UUID.randomUUID(), 1, UUID.randomUUID(), "Jane Doe", "jane.doe@spendsync.com", 1, ApprovalStepStatus.PENDING, null, null)),
                Instant.now(),
                null
        );

        sampleSummaryResponse = new RequisitionSummaryResponse(
                prId,
                "PR-2026-00001",
                "Developer Workstations",
                UUID.randomUUID(),
                "Ali Demir",
                "Engineering",
                "SpendSync Turkey",
                RequisitionStatus.PENDING_APPROVAL,
                new BigDecimal("50000.00"),
                "TRY",
                1,
                false,
                Instant.now()
        );
    }

    @Test
    @DisplayName("POST /api/v1/requisitions - creates and submits PR returning 201 Created")
    void shouldCreateAndSubmitRequisition() throws Exception {
        CreateLineItemRequest itemReq = new CreateLineItemRequest(
                "MacBook", "HARDWARE", new BigDecimal("1.0"), "PCS", new BigDecimal("50000.00"), LocalDate.now().plusDays(7)
        );
        CreateRequisitionRequest request = new CreateRequisitionRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Developer Workstations", "Hardware refresh", "TRY", List.of(itemReq)
        );

        when(requisitionService.createAndSubmitRequisition(any(CreateRequisitionRequest.class))).thenReturn(sampleDetailResponse);

        mockMvc.perform(post(Endpoints.Requisition.BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(prId.toString()))
                .andExpect(jsonPath("$.requisitionNumber").value("PR-2026-00001"))
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.totalAmount").value(50000.00));
    }

    @Test
    @DisplayName("GET /api/v1/requisitions/my-requisitions - returns user's requisitions")
    void shouldGetMyRequisitions() throws Exception {
        when(requisitionService.getMyRequisitions()).thenReturn(List.of(sampleSummaryResponse));

        mockMvc.perform(get(Endpoints.Requisition.BASE + Endpoints.Requisition.MY_REQUISITIONS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requisitionNumber").value("PR-2026-00001"))
                .andExpect(jsonPath("$[0].requisitionerName").value("Ali Demir"));
    }

    @Test
    @DisplayName("GET /api/v1/requisitions/pending-approvals - returns pending approvals for approver")
    void shouldGetMyPendingApprovals() throws Exception {
        when(requisitionService.getMyPendingApprovals()).thenReturn(List.of(sampleDetailResponse));

        mockMvc.perform(get(Endpoints.Requisition.BASE + Endpoints.Requisition.PENDING_APPROVALS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(prId.toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING_APPROVAL"));
    }

    @Test
    @DisplayName("GET /api/v1/requisitions/{id} - returns requisition details by ID")
    void shouldGetRequisitionById() throws Exception {
        when(requisitionService.getRequisitionById(prId)).thenReturn(sampleDetailResponse);

        mockMvc.perform(get(Endpoints.Requisition.BASE + "/" + prId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(prId.toString()))
                .andExpect(jsonPath("$.title").value("Developer Workstations"));
    }

    @Test
    @DisplayName("POST /api/v1/requisitions/{id}/approve - approves active step")
    void shouldApproveRequisitionStep() throws Exception {
        ApproveRequisitionStepRequest request = new ApproveRequisitionStepRequest("Approved");

        RequisitionDetailResponse approvedResponse = new RequisitionDetailResponse(
                prId, sampleDetailResponse.requisitionNumber(), sampleDetailResponse.requisitionerId(),
                sampleDetailResponse.requisitionerName(), sampleDetailResponse.requisitionerEmail(),
                sampleDetailResponse.legalEntityId(), sampleDetailResponse.legalEntityName(),
                sampleDetailResponse.costCenterId(), sampleDetailResponse.costCenterName(), sampleDetailResponse.costCenterCode(),
                sampleDetailResponse.deliveryFacilityId(), sampleDetailResponse.deliveryFacilityName(),
                sampleDetailResponse.budgetPoolId(), RequisitionStatus.APPROVED, sampleDetailResponse.totalAmount(),
                sampleDetailResponse.currency(), sampleDetailResponse.title(), sampleDetailResponse.justification(),
                null, null, sampleDetailResponse.lineItems(), sampleDetailResponse.approvalSteps(),
                Instant.now(), Instant.now()
        );

        when(requisitionService.approveStep(eq(prId), any(ApproveRequisitionStepRequest.class))).thenReturn(approvedResponse);

        mockMvc.perform(post(Endpoints.Requisition.BASE + "/" + prId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("POST /api/v1/requisitions/{id}/reject - rejects requisition")
    void shouldRejectRequisition() throws Exception {
        RejectRequisitionRequest request = new RejectRequisitionRequest("Over budget");

        RequisitionDetailResponse rejectedResponse = new RequisitionDetailResponse(
                prId, sampleDetailResponse.requisitionNumber(), sampleDetailResponse.requisitionerId(),
                sampleDetailResponse.requisitionerName(), sampleDetailResponse.requisitionerEmail(),
                sampleDetailResponse.legalEntityId(), sampleDetailResponse.legalEntityName(),
                sampleDetailResponse.costCenterId(), sampleDetailResponse.costCenterName(), sampleDetailResponse.costCenterCode(),
                sampleDetailResponse.deliveryFacilityId(), sampleDetailResponse.deliveryFacilityName(),
                sampleDetailResponse.budgetPoolId(), RequisitionStatus.REJECTED, sampleDetailResponse.totalAmount(),
                sampleDetailResponse.currency(), sampleDetailResponse.title(), sampleDetailResponse.justification(),
                "Over budget", null, sampleDetailResponse.lineItems(), sampleDetailResponse.approvalSteps(),
                Instant.now(), null
        );

        when(requisitionService.rejectRequisition(eq(prId), any(RejectRequisitionRequest.class))).thenReturn(rejectedResponse);

        mockMvc.perform(post(Endpoints.Requisition.BASE + "/" + prId + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Over budget"));
    }

    @Test
    @DisplayName("POST /api/v1/requisitions/{id}/cancel - cancels requisition")
    void shouldCancelRequisition() throws Exception {
        RequisitionDetailResponse cancelledResponse = new RequisitionDetailResponse(
                prId, sampleDetailResponse.requisitionNumber(), sampleDetailResponse.requisitionerId(),
                sampleDetailResponse.requisitionerName(), sampleDetailResponse.requisitionerEmail(),
                sampleDetailResponse.legalEntityId(), sampleDetailResponse.legalEntityName(),
                sampleDetailResponse.costCenterId(), sampleDetailResponse.costCenterName(), sampleDetailResponse.costCenterCode(),
                sampleDetailResponse.deliveryFacilityId(), sampleDetailResponse.deliveryFacilityName(),
                sampleDetailResponse.budgetPoolId(), RequisitionStatus.CANCELLED, sampleDetailResponse.totalAmount(),
                sampleDetailResponse.currency(), sampleDetailResponse.title(), sampleDetailResponse.justification(),
                null, null, sampleDetailResponse.lineItems(), sampleDetailResponse.approvalSteps(),
                Instant.now(), null
        );

        when(requisitionService.cancelRequisition(prId)).thenReturn(cancelledResponse);

        mockMvc.perform(post(Endpoints.Requisition.BASE + "/" + prId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("GET /api/v1/requisitions - returns all requisitions")
    void shouldGetAllRequisitions() throws Exception {
        when(requisitionService.getAllRequisitions(RequisitionStatus.PENDING_APPROVAL))
                .thenReturn(List.of(sampleSummaryResponse));

        mockMvc.perform(get(Endpoints.Requisition.BASE)
                        .param("status", "PENDING_APPROVAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(prId.toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING_APPROVAL"));
    }
}
