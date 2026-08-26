package com.enterprise.spendsync.shared.security;

import com.enterprise.spendsync.core.internal.domain.RolePermissionRegistry;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.shared.config.OpenApiConfig;
import com.enterprise.spendsync.shared.config.SecurityConfig;
import com.enterprise.spendsync.shared.tenant.TenantFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.availability.AvailabilityHealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.availability.AvailabilityProbesAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.servlet.WebMvcEndpointManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.availability.ApplicationAvailabilityAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = ActuatorSecurityIntegrationTest.TestSecurityAppConfig.class,
        properties = {
                "spendsync.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970337336763979244226452948404D6351655468576D5A7134743777217A25432A",
                "management.endpoints.web.exposure.include=health,info,metrics",
                "management.endpoint.health.show-details=when_authorized",
                "management.endpoint.health.probes.enabled=true",
                "management.health.livenessstate.enabled=true",
                "management.health.readinessstate.enabled=true"
        }
)
@AutoConfigureMockMvc
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        DispatcherServletAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        ApplicationAvailabilityAutoConfiguration.class,
        AvailabilityProbesAutoConfiguration.class,
        AvailabilityHealthContributorAutoConfiguration.class,
        MetricsAutoConfiguration.class,
        SimpleMetricsExportAutoConfiguration.class,
        EndpointAutoConfiguration.class,
        WebEndpointAutoConfiguration.class,
        HealthEndpointAutoConfiguration.class,
        HealthContributorAutoConfiguration.class,
        InfoEndpointAutoConfiguration.class,
        MetricsEndpointAutoConfiguration.class,
        ManagementContextAutoConfiguration.class,
        ServletManagementContextAutoConfiguration.class,
        WebMvcEndpointManagementContextConfiguration.class
})
@DisplayName("Actuator & Swagger Endpoint Security Hardening Tests")
class ActuatorSecurityIntegrationTest {

    @Configuration
    @Import({
            SecurityConfig.class,
            TenantFilter.class,
            JwtAuthenticationFilter.class,
            JwtTokenProvider.class,
            RolePermissionRegistry.class,
            OpenApiConfig.class
    })
    static class TestSecurityAppConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String rootUserToken;
    private String requisitionerToken;

    @BeforeEach
    void setUp() {
        UUID tenantId = UUID.randomUUID();

        com.enterprise.spendsync.core.internal.domain.User rootUser = new com.enterprise.spendsync.core.internal.domain.User();
        rootUser.setId(UUID.randomUUID());
        rootUser.setEmail("root.actuator@test.com");
        rootUser.setFirstName("Root");
        rootUser.setLastName("Admin");
        rootUser.setActive(true);
        rootUser.setRoles(Set.of(RoleType.ROOT_USER));

        com.enterprise.spendsync.core.internal.domain.Tenant tenant = new com.enterprise.spendsync.core.internal.domain.Tenant("Test", "test");
        tenant.setId(tenantId);
        rootUser.setTenant(tenant);

        com.enterprise.spendsync.core.internal.domain.User requisitioner = new com.enterprise.spendsync.core.internal.domain.User();
        requisitioner.setId(UUID.randomUUID());
        requisitioner.setEmail("req.actuator@test.com");
        requisitioner.setFirstName("Requisitioner");
        requisitioner.setLastName("User");
        requisitioner.setActive(true);
        requisitioner.setRoles(Set.of(RoleType.REQUISITIONER));
        requisitioner.setTenant(tenant);

        rootUserToken = "Bearer " + jwtTokenProvider.generateAccessToken(rootUser);
        requisitionerToken = "Bearer " + jwtTokenProvider.generateAccessToken(requisitioner);
    }

    @Test
    @DisplayName("Anonymous user should be able to access /actuator/health and probes")
    void anonymousCanAccessHealthAndProbes() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());

        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Anonymous user should receive 403 Forbidden when accessing sensitive actuator endpoints")
    void anonymousCannotAccessSensitiveActuatorEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Anonymous user should receive 403 Forbidden when accessing Swagger and OpenAPI docs")
    void anonymousCannotAccessSwaggerAndApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROOT_USER should be able to access /actuator/metrics")
    void rootUserCanAccessProtectedActuatorAndDocs() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .header("Authorization", rootUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }

    @Test
    @DisplayName("Non-root user (e.g. REQUISITIONER) should receive 403 Forbidden when accessing metrics or api docs")
    void nonRootUserIsForbiddenFromActuatorMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .header("Authorization", requisitionerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/v3/api-docs")
                        .header("Authorization", requisitionerToken))
                .andExpect(status().isForbidden());
    }
}
