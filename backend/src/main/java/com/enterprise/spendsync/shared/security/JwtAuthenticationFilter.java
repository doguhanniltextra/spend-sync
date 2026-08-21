package com.enterprise.spendsync.shared.security;

import com.enterprise.spendsync.shared.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // If TenantContext is not yet populated by header, populate from JWT claims
            if (authentication.getPrincipal() instanceof UserPrincipal principal) {
                if (principal.getId() != null) {
                    org.slf4j.MDC.put(com.enterprise.spendsync.shared.filter.MdcLoggingFilter.MDC_USER_ID, principal.getId().toString());
                }
                if (principal.getRoles() != null && !principal.getRoles().isEmpty()) {
                    org.slf4j.MDC.put(com.enterprise.spendsync.shared.filter.MdcLoggingFilter.MDC_USER_ROLE, principal.getRoles().iterator().next().name());
                }
                if (principal.getTenantId() != null) {
                    org.slf4j.MDC.put(com.enterprise.spendsync.shared.filter.MdcLoggingFilter.MDC_TENANT_ID, principal.getTenantId().toString());
                    if (TenantContext.getTenantId() == null) {
                        TenantContext.setTenantId(principal.getTenantId());
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
