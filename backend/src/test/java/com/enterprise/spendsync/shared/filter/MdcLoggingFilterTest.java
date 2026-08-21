package com.enterprise.spendsync.shared.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MDC Logging Filter Unit Tests")
class MdcLoggingFilterTest {

    private MdcLoggingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new MdcLoggingFilter();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Should extract existing X-Trace-Id header and inject into MDC and response")
    void shouldExtractExistingTraceId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/invoices");
        request.addHeader("X-Trace-Id", "trace-abc-123");
        request.setRemoteAddr("10.0.0.1");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            assertThat(MDC.get(MdcLoggingFilter.MDC_TRACE_ID)).isEqualTo("trace-abc-123");
            assertThat(MDC.get(MdcLoggingFilter.MDC_SPAN_ID)).isNotBlank().hasSize(8);
            assertThat(MDC.get(MdcLoggingFilter.MDC_CLIENT_IP)).isEqualTo("10.0.0.1");
            assertThat(MDC.get(MdcLoggingFilter.MDC_HTTP_METHOD)).isEqualTo("POST");
            assertThat(MDC.get(MdcLoggingFilter.MDC_REQUEST_URI)).isEqualTo("/api/v1/invoices");
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("trace-abc-123");
        // Verify MDC cleaned after request execution
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Should generate new traceId when header is missing and extract X-Forwarded-For IP")
    void shouldGenerateNewTraceIdAndExtractForwardedIp() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/catalog");
        request.addHeader("X-Forwarded-For", "203.0.113.195, 198.51.100.1");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            assertThat(MDC.get(MdcLoggingFilter.MDC_TRACE_ID)).isNotBlank();
            assertThat(MDC.get(MdcLoggingFilter.MDC_CLIENT_IP)).isEqualTo("203.0.113.195");
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Trace-Id")).isNotBlank();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Should fallback to X-Request-Id when X-Trace-Id is not present")
    void shouldFallbackToRequestId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        request.addHeader("X-Request-Id", "req-7788");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            assertThat(MDC.get(MdcLoggingFilter.MDC_TRACE_ID)).isEqualTo("req-7788");
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("req-7788");
    }
}
