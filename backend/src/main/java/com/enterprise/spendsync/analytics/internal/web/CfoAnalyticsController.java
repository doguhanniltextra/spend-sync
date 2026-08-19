package com.enterprise.spendsync.analytics.internal.web;

import com.enterprise.spendsync.analytics.dto.CfoExecutiveDeckResponse;
import com.enterprise.spendsync.analytics.internal.service.CfoAnalyticsService;
import com.enterprise.spendsync.shared.config.Endpoints;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Endpoints.Analytics.BASE)
@Tag(name = "CFO Analytics & Executive Deck", description = "Live financial metrics, cash outflow forecast, and supplier concentration analytics")
public class CfoAnalyticsController {

    private final CfoAnalyticsService cfoAnalyticsService;

    public CfoAnalyticsController(CfoAnalyticsService cfoAnalyticsService) {
        this.cfoAnalyticsService = cfoAnalyticsService;
    }

    @GetMapping(Endpoints.Analytics.CFO_DECK)
    @PreAuthorize("hasAnyAuthority('PERM_BUDGET_READ', 'PERM_ORG_MANAGE') or hasRole('ROOT_USER')")
    @Operation(summary = "Get live CFO executive analytics deck")
    public ResponseEntity<CfoExecutiveDeckResponse> getCfoExecutiveDeck() {
        return ResponseEntity.ok(cfoAnalyticsService.getCfoExecutiveDeck());
    }
}
