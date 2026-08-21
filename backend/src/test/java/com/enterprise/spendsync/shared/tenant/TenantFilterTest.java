package com.enterprise.spendsync.shared.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantFilter Unit Tests (Header Parsing & ThreadLocal Cleanup)")
class TenantFilterTest {

    private TenantFilter tenantFilter;
    private ObjectMapper objectMapper;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        tenantFilter = new TenantFilter(objectMapper);
        TenantContext.clear();
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should extract X-Tenant-Id header, set TenantContext and clear on finish")
    void shouldExtractTenantHeaderAndClearFinally() throws ServletException, IOException {
        UUID expectedTenantId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/requisitions");
        request.addHeader(TenantFilter.TENANT_HEADER, expectedTenantId.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<UUID> capturedTenantInChain = new AtomicReference<>();
        doAnswer(invocation -> {
            capturedTenantInChain.set(TenantContext.getRequiredTenantId());
            return null;
        }).when(filterChain).doFilter(any(), any());

        tenantFilter.doFilter(request, response, filterChain);

        // Inside the filter chain, TenantContext must hold the tenantId
        assertThat(capturedTenantInChain.get()).isEqualTo(expectedTenantId);
        // After doFilter completes, TenantContext must be completely cleared
        assertThat(TenantContext.getTenantId()).isEmpty();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Should reject malformed non-UUID X-Tenant-Id with 400 Bad Request")
    void shouldRejectMalformedTenantHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/requisitions");
        request.addHeader(TenantFilter.TENANT_HEADER, "invalid-not-a-uuid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        tenantFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("INVALID_TENANT_HEADER");
        verify(filterChain, never()).doFilter(any(), any());
        assertThat(TenantContext.getTenantId()).isEmpty();
    }

    @Test
    @DisplayName("Should ensure TenantContext is cleared even when FilterChain throws RuntimeException")
    void shouldEnsureTenantContextClearedOnException() throws ServletException, IOException {
        UUID tenantId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/orders");
        request.addHeader(TenantFilter.TENANT_HEADER, tenantId.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();

        doThrow(new RuntimeException("Simulated unexpected downstream error"))
                .when(filterChain).doFilter(any(), any());

        try {
            tenantFilter.doFilter(request, response, filterChain);
        } catch (RuntimeException ignored) {
            // Expected
        }

        // ThreadLocal MUST be cleared to prevent thread-pool leakage
        assertThat(TenantContext.getTenantId()).isEmpty();
    }

    @Test
    @DisplayName("Should allow public endpoints without X-Tenant-Id header")
    void shouldAllowPublicEndpointsWithoutTenantHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        tenantFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
