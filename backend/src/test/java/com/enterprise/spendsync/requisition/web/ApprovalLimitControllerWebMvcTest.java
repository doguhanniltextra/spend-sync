package com.enterprise.spendsync.requisition.web;

import com.enterprise.spendsync.requisition.internal.domain.ApprovalAuthorityLimit;
import com.enterprise.spendsync.requisition.internal.dto.ApprovalLimitResponse;
import com.enterprise.spendsync.requisition.internal.dto.SetApprovalLimitRequest;
import com.enterprise.spendsync.requisition.internal.service.ApprovalLimitService;
import com.enterprise.spendsync.requisition.internal.web.ApprovalLimitController;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalLimitController REST Web API Slice Tests")
class ApprovalLimitControllerWebMvcTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private ApprovalLimitService approvalLimitService;

    @InjectMocks
    private ApprovalLimitController approvalLimitController;

    private UUID limitId;
    private ApprovalLimitResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(approvalLimitController).build();
        limitId = UUID.randomUUID();

        sampleResponse = new ApprovalLimitResponse(
                limitId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Jane Doe",
                "jane.doe@spendsync.com",
                UUID.randomUUID(),
                "SpendSync Turkey",
                UUID.randomUUID(),
                "Engineering",
                1,
                BigDecimal.ZERO,
                new BigDecimal("50000.00"),
                false,
                "TRY",
                true,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("POST /api/v1/requisitions/approval-limits - creates limit and returns 201 Created")
    void shouldSetApprovalLimit() throws Exception {
        SetApprovalLimitRequest request = new SetApprovalLimitRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                BigDecimal.ZERO,
                new BigDecimal("50000.00"),
                "TRY"
        );

        when(approvalLimitService.setApprovalLimit(any(SetApprovalLimitRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post(Endpoints.Requisition.BASE + Endpoints.Requisition.APPROVAL_LIMITS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(limitId.toString()))
                .andExpect(jsonPath("$.approvalLevel").value(1))
                .andExpect(jsonPath("$.maxAmount").value(50000.00));
    }

    @Test
    @DisplayName("GET /api/v1/requisitions/approval-limits - returns list of limits")
    void shouldGetAllLimits() throws Exception {
        when(approvalLimitService.getAllLimits(any(), any())).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get(Endpoints.Requisition.BASE + Endpoints.Requisition.APPROVAL_LIMITS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(limitId.toString()))
                .andExpect(jsonPath("$[0].userFullName").value("Jane Doe"));
    }

    @Test
    @DisplayName("GET /api/v1/requisitions/approval-limits/{id} - returns limit by ID")
    void shouldGetApprovalLimitById() throws Exception {
        when(approvalLimitService.getApprovalLimitById(limitId)).thenReturn(sampleResponse);

        mockMvc.perform(get(Endpoints.Requisition.BASE + "/approval-limits/" + limitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(limitId.toString()))
                .andExpect(jsonPath("$.currency").value("TRY"));
    }

    @Test
    @DisplayName("PATCH /api/v1/requisitions/approval-limits/{id}/status - toggles active status")
    void shouldToggleLimitStatus() throws Exception {
        ApprovalLimitResponse updated = new ApprovalLimitResponse(
                limitId, sampleResponse.tenantId(), sampleResponse.userId(), sampleResponse.userFullName(),
                sampleResponse.userEmail(), sampleResponse.legalEntityId(), sampleResponse.legalEntityName(),
                sampleResponse.costCenterId(), sampleResponse.costCenterName(), sampleResponse.approvalLevel(),
                sampleResponse.minAmount(), sampleResponse.maxAmount(), sampleResponse.isUnlimited(),
                sampleResponse.currency(), false, sampleResponse.createdAt(), Instant.now()
        );

        when(approvalLimitService.toggleLimitStatus(eq(limitId), eq(false))).thenReturn(updated);

        mockMvc.perform(patch(Endpoints.Requisition.BASE + "/approval-limits/" + limitId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/requisitions/approval-limits/effective - returns effective limit details")
    void shouldGetEffectiveLimit() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID legalEntityId = UUID.randomUUID();
        UUID costCenterId = UUID.randomUUID();

        when(approvalLimitService.getEffectiveLimitDetails(userId, legalEntityId, costCenterId))
                .thenReturn(Optional.empty());

        mockMvc.perform(get(Endpoints.Requisition.BASE + Endpoints.Requisition.EFFECTIVE_LIMIT)
                        .param("userId", userId.toString())
                        .param("legalEntityId", legalEntityId.toString())
                        .param("costCenterId", costCenterId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasConfiguredLimit").value(false))
                .andExpect(jsonPath("$.isUnlimited").value(false));
    }
}
