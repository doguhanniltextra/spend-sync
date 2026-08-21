package com.enterprise.spendsync.shared.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that establishes Mapped Diagnostic Context (MDC) tracing keys
 * at the very entry of every HTTP request, allowing distributed trace correlation across the stack.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_SPAN_ID = "spanId";
    public static final String MDC_CLIENT_IP = "clientIp";
    public static final String MDC_HTTP_METHOD = "httpMethod";
    public static final String MDC_REQUEST_URI = "requestUri";
    public static final String MDC_TENANT_ID = "tenantId";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_USER_ROLE = "userRole";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String traceId = resolveTraceId(request);
            String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

            MDC.put(MDC_TRACE_ID, traceId);
            MDC.put(MDC_SPAN_ID, spanId);
            MDC.put(MDC_CLIENT_IP, resolveClientIp(request));
            MDC.put(MDC_HTTP_METHOD, request.getMethod());
            MDC.put(MDC_REQUEST_URI, request.getRequestURI());

            // Echo trace ID back in response headers for client/frontend correlation
            response.setHeader(TRACE_ID_HEADER, traceId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (StringUtils.hasText(traceId)) {
            return traceId.trim();
        }
        String reqId = request.getHeader(REQUEST_ID_HEADER);
        if (StringUtils.hasText(reqId)) {
            return reqId.trim();
        }
        return UUID.randomUUID().toString();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "UNKNOWN_IP";
    }
}
