package com.enterprise.spendsync.intelligence.internal.service;

import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.intelligence.dto.BudgetRunwayAnalysisDto;
import com.enterprise.spendsync.intelligence.dto.CashDiscountOpportunityDto;
import com.enterprise.spendsync.intelligence.dto.CopilotQueryRequest;
import com.enterprise.spendsync.intelligence.dto.CopilotQueryResponse;
import com.enterprise.spendsync.intelligence.dto.ExecutivePulseResponse;
import com.enterprise.spendsync.intelligence.dto.WhatIfBudgetImpactRequest;
import com.enterprise.spendsync.intelligence.dto.WhatIfBudgetImpactResponse;

import java.util.List;
import java.util.UUID;

public interface SpendIntelligenceService {

    ExecutivePulseResponse getExecutivePulse(UUID tenantId, User currentUser);

    List<BudgetRunwayAnalysisDto> getBudgetRunwayAnalysis(UUID tenantId);

    List<CashDiscountOpportunityDto> getCashDiscountOpportunities(UUID tenantId);

    WhatIfBudgetImpactResponse simulateWhatIfBudgetImpact(UUID tenantId, WhatIfBudgetImpactRequest request);

    CopilotQueryResponse askCopilot(UUID tenantId, User currentUser, CopilotQueryRequest request);
}
