package com.enterprise.spendsync.analytics.web;

import com.enterprise.spendsync.analytics.dto.CategorySpendDto;
import com.enterprise.spendsync.analytics.dto.CfoExecutiveDeckResponse;
import com.enterprise.spendsync.analytics.dto.MonthlyOutflowDto;
import com.enterprise.spendsync.analytics.dto.ThreeWayMatchIntegrityDto;
import com.enterprise.spendsync.analytics.dto.TopVendorSpendDto;
import com.enterprise.spendsync.analytics.internal.service.CfoAnalyticsService;
import com.enterprise.spendsync.analytics.internal.web.CfoAnalyticsController;
import com.enterprise.spendsync.shared.config.Endpoints;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("CfoAnalyticsController REST Web API Slice Tests (Executive Deck)")
class CfoAnalyticsControllerWebMvcTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private CfoAnalyticsService cfoAnalyticsService;

    @InjectMocks
    private CfoAnalyticsController cfoAnalyticsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cfoAnalyticsController).build();
    }

    @Test
    @DisplayName("GET /api/v1/analytics/cfo-deck - returns CFO executive deck analytics")
    void shouldGetCfoExecutiveDeck() throws Exception {
        CfoExecutiveDeckResponse response = new CfoExecutiveDeckResponse(
                new BigDecimal("400000.00"),
                new BigDecimal("500000.00"),
                new BigDecimal("1000000.00"),
                50.0,
                "TRY",
                List.of(new CategorySpendDto("IT_HARDWARE", new BigDecimal("250000.00"), 100.0)),
                List.of(new MonthlyOutflowDto("2026-08", new BigDecimal("150000.00"), new BigDecimal("120000.00"), new BigDecimal("270000.00"))),
                List.of(new TopVendorSpendDto(UUID.randomUUID(), "AWS EMEA", "1112223334", "TIER_1_STRATEGIC", new BigDecimal("250000.00"), 100.0, "HIGH")),
                new ThreeWayMatchIntegrityDto(10L, 8L, 2L, 80.0, new BigDecimal("50000.00"))
        );

        when(cfoAnalyticsService.getCfoExecutiveDeck()).thenReturn(response);

        mockMvc.perform(get(Endpoints.Analytics.BASE + Endpoints.Analytics.CFO_DECK))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAllocatedBudget").value(1000000.00))
                .andExpect(jsonPath("$.overallBudgetUtilizationPercent").value(50.0))
                .andExpect(jsonPath("$.matchIntegrity.firstTimeMatchRatePercent").value(80.0));
    }
}
