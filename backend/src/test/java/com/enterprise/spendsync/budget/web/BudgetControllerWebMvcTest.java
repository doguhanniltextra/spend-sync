package com.enterprise.spendsync.budget.web;

import com.enterprise.spendsync.budget.internal.domain.BudgetEnforcementMode;
import com.enterprise.spendsync.budget.internal.domain.BudgetPeriodType;
import com.enterprise.spendsync.budget.internal.domain.BudgetStatus;
import com.enterprise.spendsync.budget.internal.domain.BudgetTransactionType;
import com.enterprise.spendsync.budget.internal.dto.*;
import com.enterprise.spendsync.budget.internal.service.BudgetService;
import com.enterprise.spendsync.budget.internal.web.BudgetController;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("BudgetController REST Web API Slice Tests")
class BudgetControllerWebMvcTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private BudgetService budgetService;

    @InjectMocks
    private BudgetController budgetController;

    private UUID poolId;
    private BudgetPoolResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(budgetController).build();
        poolId = UUID.randomUUID();

        sampleResponse = new BudgetPoolResponse(
                poolId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SpendSync Turkey",
                UUID.randomUUID(),
                "Engineering",
                "CC-ENG",
                2026,
                BudgetPeriodType.ANNUAL,
                "ANNUAL",
                BudgetStatus.ACTIVE,
                BudgetEnforcementMode.HARD_STOP,
                BigDecimal.ZERO,
                new BigDecimal("500000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("500000.00"),
                new BigDecimal("500000.00"),
                "TRY",
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("POST /api/v1/budget/pools - creates budget pool and returns 201 Created")
    void shouldCreateBudgetPool() throws Exception {
        CreateBudgetPoolRequest request = new CreateBudgetPoolRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                2026,
                BudgetPeriodType.ANNUAL,
                "ANNUAL",
                BudgetStatus.ACTIVE,
                BudgetEnforcementMode.HARD_STOP,
                BigDecimal.ZERO,
                new BigDecimal("500000.00"),
                "TRY"
        );

        when(budgetService.createBudgetPool(any(CreateBudgetPoolRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post(Endpoints.Budget.BASE + Endpoints.Budget.POOLS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(poolId.toString()))
                .andExpect(jsonPath("$.costCenterName").value("Engineering"))
                .andExpect(jsonPath("$.fiscalYear").value(2026))
                .andExpect(jsonPath("$.allocatedAmount").value(500000.00));
    }

    @Test
    @DisplayName("GET /api/v1/budget/pools - returns 200 OK and list of budget pools")
    void shouldGetAllBudgetPools() throws Exception {
        when(budgetService.getAllBudgetPools(eq(2026), eq(BudgetStatus.ACTIVE)))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get(Endpoints.Budget.BASE + Endpoints.Budget.POOLS)
                        .param("fiscalYear", "2026")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(poolId.toString()))
                .andExpect(jsonPath("$[0].currency").value("TRY"));
    }

    @Test
    @DisplayName("GET /api/v1/budget/pools/{id} - returns 200 OK and single budget pool details")
    void shouldGetBudgetPoolById() throws Exception {
        when(budgetService.getBudgetPoolById(poolId)).thenReturn(sampleResponse);

        mockMvc.perform(get(Endpoints.Budget.BASE + "/pools/" + poolId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(poolId.toString()))
                .andExpect(jsonPath("$.costCenterCode").value("CC-ENG"));
    }

    @Test
    @DisplayName("PATCH /api/v1/budget/pools/{id}/status - updates pool status")
    void shouldUpdateBudgetStatus() throws Exception {
        UpdateBudgetStatusRequest request = new UpdateBudgetStatusRequest(BudgetStatus.FROZEN);

        BudgetPoolResponse frozenResponse = new BudgetPoolResponse(
                poolId, sampleResponse.tenantId(), sampleResponse.legalEntityId(), sampleResponse.legalEntityName(),
                sampleResponse.costCenterId(), sampleResponse.costCenterName(), sampleResponse.costCenterCode(),
                2026, BudgetPeriodType.ANNUAL, "ANNUAL", BudgetStatus.FROZEN, BudgetEnforcementMode.HARD_STOP,
                BigDecimal.ZERO, new BigDecimal("500000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("500000.00"), new BigDecimal("500000.00"), "TRY", Instant.now(), Instant.now()
        );

        when(budgetService.updateBudgetStatus(eq(poolId), any(UpdateBudgetStatusRequest.class))).thenReturn(frozenResponse);

        mockMvc.perform(patch(Endpoints.Budget.BASE + "/pools/" + poolId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));
    }

    @Test
    @DisplayName("PATCH /api/v1/budget/pools/{id}/adjust - adjusts allocation amount")
    void shouldAdjustBudget() throws Exception {
        AdjustBudgetRequest request = new AdjustBudgetRequest(new BigDecimal("600000.00"), "Q2 expansion");

        BudgetPoolResponse adjustedResponse = new BudgetPoolResponse(
                poolId, sampleResponse.tenantId(), sampleResponse.legalEntityId(), sampleResponse.legalEntityName(),
                sampleResponse.costCenterId(), sampleResponse.costCenterName(), sampleResponse.costCenterCode(),
                2026, BudgetPeriodType.ANNUAL, "ANNUAL", BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP,
                BigDecimal.ZERO, new BigDecimal("600000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("600000.00"), new BigDecimal("600000.00"), "TRY", Instant.now(), Instant.now()
        );

        when(budgetService.adjustBudget(eq(poolId), any(AdjustBudgetRequest.class))).thenReturn(adjustedResponse);

        mockMvc.perform(patch(Endpoints.Budget.BASE + "/pools/" + poolId + "/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocatedAmount").value(600000.00));
    }

    @Test
    @DisplayName("GET /api/v1/budget/pools/{id}/transactions - returns ledger transaction history")
    void shouldGetTransactionsForPool() throws Exception {
        BudgetTransactionResponse tx = new BudgetTransactionResponse(
                UUID.randomUUID(),
                poolId,
                BudgetTransactionType.INITIAL_ALLOCATION,
                new BigDecimal("500000.00"),
                BigDecimal.ZERO,
                new BigDecimal("500000.00"),
                poolId,
                "INITIAL_SETUP",
                "Initial allocation",
                Instant.now()
        );

        when(budgetService.getTransactionsForPool(poolId)).thenReturn(List.of(tx));

        mockMvc.perform(get(Endpoints.Budget.BASE + "/pools/" + poolId + "/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionType").value("INITIAL_ALLOCATION"))
                .andExpect(jsonPath("$[0].amount").value(500000.00));
    }

    @Test
    @DisplayName("POST /api/v1/budget/transfers - transfers funds between pools and returns 204 No Content")
    void shouldTransferBudget() throws Exception {
        BudgetTransferRequest request = new BudgetTransferRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("50000.00"),
                "Rebalancing"
        );

        mockMvc.perform(post(Endpoints.Budget.BASE + Endpoints.Budget.TRANSFERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(budgetService).transferBudget(any(BudgetTransferRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/budget/summary - returns fiscal year budget summary")
    void shouldGetBudgetSummary() throws Exception {
        BudgetSummaryResponse summary = new BudgetSummaryResponse(
                2026,
                1,
                new BigDecimal("500000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("500000.00"),
                List.of(sampleResponse)
        );

        when(budgetService.getBudgetSummary(2026)).thenReturn(summary);

        mockMvc.perform(get(Endpoints.Budget.BASE + Endpoints.Budget.SUMMARY)
                        .param("fiscalYear", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiscalYear").value(2026))
                .andExpect(jsonPath("$.totalPools").value(1))
                .andExpect(jsonPath("$.totalAllocated").value(500000.00));
    }
}
