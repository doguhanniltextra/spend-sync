package com.enterprise.spendsync.shared.tenant;

import com.enterprise.spendsync.shared.config.Endpoints;
import com.enterprise.spendsync.shared.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Filter that intercepts incoming HTTP requests, resolves the X-Tenant-Id header,
 * and binds the Tenant ID to the thread-local TenantContext.
 */
@Component
public class TenantFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-Id";

    private static final List<String> PUBLIC_PATH_PATTERNS = List.of(
            Endpoints.Auth.BASE + "/**",
            Endpoints.Organization.BASE + Endpoints.Organization.CREATE_COMPANY,
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/**",
            "/error"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper;

    public TenantFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String tenantHeaderValue = request.getHeader(TENANT_HEADER);

        boolean isPublicPath = isPublicEndpoint(requestPath);

        try {
            if (tenantHeaderValue != null && !tenantHeaderValue.isBlank()) {
                try {
                    UUID tenantId = UUID.fromString(tenantHeaderValue.trim());
                    TenantContext.setTenantId(tenantId);
                } catch (IllegalArgumentException e) {
                    sendErrorResponse(response, HttpStatus.BAD_REQUEST, "INVALID_TENANT_HEADER",
                            "The '" + TENANT_HEADER + "' header must be a valid UUID.");
                    return;
                }
            } else if (!isPublicPath) {
                // If it's a protected enterprise endpoint and tenant header is missing
                sendErrorResponse(response, HttpStatus.BAD_REQUEST, "MISSING_TENANT_HEADER",
                        "The '" + TENANT_HEADER + "' header is required for accessing protected enterprise endpoints.");
                return;
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean isPublicEndpoint(String path) {
        for (String pattern : PUBLIC_PATH_PATTERNS) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String errorCode, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(errorCode, message, status.value());
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
