package com.enterprise.spendsync.core.internal.web;

import com.enterprise.spendsync.core.internal.dto.CompanyResponse;
import com.enterprise.spendsync.core.internal.dto.CreateCompanyRequest;
import com.enterprise.spendsync.core.internal.service.CompanyService;
import com.enterprise.spendsync.shared.config.Endpoints;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Organization & Hierarchy Management REST Controller.
 */
@RestController
@RequestMapping(Endpoints.Organization.BASE)
public class OrganizationController {

    private final CompanyService companyService;

    public OrganizationController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping(Endpoints.Organization.CREATE_COMPANY)
    public ResponseEntity<CompanyResponse> createCompany(@Valid @RequestBody CreateCompanyRequest request) {
        CompanyResponse response = companyService.createCompany(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(Endpoints.Organization.CURRENT_CONTEXT)
    public ResponseEntity<Map<String, Object>> getCurrentTenantContext() {
        UUID currentTenantId = TenantContext.getRequiredTenantId();
        return ResponseEntity.ok(Map.of(
                "tenantId", currentTenantId,
                "status", "ACTIVE_CONTEXT"
        ));
    }
}
