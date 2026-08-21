package com.enterprise.spendsync.shared.tenant;

import com.enterprise.spendsync.shared.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TenantFilterAndSecurityNegativeTest {

    private TenantFilter tenantFilter;
    private ObjectMapper objectMapper;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        tenantFilter = new TenantFilter(objectMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should extract valid UUID from X-Tenant-Id header and bind to TenantContext")
    void shouldBindValidTenantHeader() throws ServletException, IOException {
        UUID tenantId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/requisitions");
        request.addHeader("X-Tenant-Id", tenantId.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();

        tenantFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Should return 400 Bad Request when X-Tenant-Id is an invalid UUID")
    void shouldReturnBadRequestForInvalidTenantHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/requisitions");
        request.addHeader("X-Tenant-Id", "not-a-valid-uuid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        tenantFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("INVALID_TENANT_HEADER");
    }

    @Test
    @DisplayName("Should fallback to UserPrincipal tenantId when header is absent")
    void shouldFallbackToUserPrincipalTenantId() throws ServletException, IOException {
        UUID principalTenantId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                UUID.randomUUID(), principalTenantId, "user@test.com", "pass", "Full Name",
                true, Set.of(com.enterprise.spendsync.core.internal.domain.RoleType.ROOT_USER), java.util.Collections.emptySet()
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/requisitions");
        MockHttpServletResponse response = new MockHttpServletResponse();

        tenantFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should allow public endpoints without X-Tenant-Id header")
    void shouldAllowPublicEndpoints() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        tenantFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
