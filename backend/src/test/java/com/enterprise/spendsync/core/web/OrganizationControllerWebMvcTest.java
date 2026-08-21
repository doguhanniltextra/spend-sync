package com.enterprise.spendsync.core.web;

import com.enterprise.spendsync.core.internal.dto.CompanyResponse;
import com.enterprise.spendsync.core.internal.dto.CreateCompanyRequest;
import com.enterprise.spendsync.core.internal.service.CompanyService;
import com.enterprise.spendsync.core.internal.web.OrganizationController;
import com.enterprise.spendsync.shared.config.Endpoints;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationController REST Web API Slice Tests")
class OrganizationControllerWebMvcTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CompanyService companyService;

    @InjectMocks
    private OrganizationController organizationController;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(organizationController).build();
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("POST /api/v1/organization/companies - creates company and returns 201 Created")
    void shouldCreateCompanySuccessfully() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID legalEntityId = UUID.randomUUID();

        CreateCompanyRequest request = new CreateCompanyRequest(
                userId,
                "SpendSync Inc.",
                "SpendSync Legal Entity",
                "COMP01",
                "1234567890",
                "Delaware",
                "USD",
                "100 Main St, DE",
                "US"
        );

        CompanyResponse companyResponse = new CompanyResponse(
                tenantId,
                "SpendSync Inc.",
                "spendsync-inc",
                "ENTERPRISE",
                legalEntityId,
                "SpendSync Legal Entity",
                "COMP01",
                "1234567890",
                "Delaware",
                "USD",
                "US",
                userId,
                Instant.now()
        );

        when(companyService.createCompany(any(CreateCompanyRequest.class))).thenReturn(companyResponse);

        mockMvc.perform(post(Endpoints.Organization.BASE + Endpoints.Organization.CREATE_COMPANY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.tenantName").value("SpendSync Inc."));
    }

    @Test
    @DisplayName("GET /api/v1/organization/context - returns current active tenant context")
    void shouldGetCurrentTenantContext() throws Exception {
        mockMvc.perform(get(Endpoints.Organization.BASE + Endpoints.Organization.CURRENT_CONTEXT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE_CONTEXT"));
    }
}
