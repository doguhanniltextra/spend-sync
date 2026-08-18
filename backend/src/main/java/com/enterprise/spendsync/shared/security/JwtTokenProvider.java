package com.enterprise.spendsync.shared.security;

import com.enterprise.spendsync.core.internal.domain.RolePermissionRegistry;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKey key;
    private final long accessTokenExpirySeconds;
    private final long refreshTokenExpiryDays;
    private final RolePermissionRegistry rolePermissionRegistry;

    public JwtTokenProvider(
            @Value("${spendsync.jwt.secret}") String secret,
            @Value("${spendsync.jwt.access-token-expiry-seconds:900}") long accessTokenExpirySeconds,
            @Value("${spendsync.jwt.refresh-token-expiry-days:7}") long refreshTokenExpiryDays,
            RolePermissionRegistry rolePermissionRegistry) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirySeconds = accessTokenExpirySeconds;
        this.refreshTokenExpiryDays = refreshTokenExpiryDays;
        this.rolePermissionRegistry = rolePermissionRegistry;
    }

    /**
     * Generates a signed JWT Access Token containing user metadata, tenantId and assigned roles.
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenExpirySeconds, ChronoUnit.SECONDS);

        List<String> roleNames = user.getRoles().stream()
                .map(RoleType::name)
                .toList();

        String tenantIdStr = user.getTenant() != null ? user.getTenant().getId().toString() : null;

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("fullName", user.getFirstName() + " " + user.getLastName())
                .claim("tenantId", tenantIdStr)
                .claim("roles", roleNames)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    /**
     * Generates a secure random 256-bit refresh token string.
     */
    public String generateRefreshTokenString() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }

    public Instant calculateRefreshTokenExpiry() {
        return Instant.now().plus(refreshTokenExpiryDays, ChronoUnit.DAYS);
    }

    public long getAccessTokenExpirySeconds() {
        return accessTokenExpirySeconds;
    }

    /**
     * Validates signature and expiration of a JWT token.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts Claims from a valid JWT token.
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Builds Spring Security Authentication object directly from JWT claims without hitting DB.
     */
    public Authentication getAuthentication(String token) {
        Claims claims = extractClaims(token);

        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);
        String fullName = claims.get("fullName", String.class);
        String tenantIdStr = claims.get("tenantId", String.class);
        UUID tenantId = tenantIdStr != null ? UUID.fromString(tenantIdStr) : null;

        @SuppressWarnings("unchecked")
        List<String> roleNames = claims.get("roles", List.class);
        Set<RoleType> roles = roleNames != null
                ? roleNames.stream().map(RoleType::valueOf).collect(Collectors.toSet())
                : Set.of();

        Set<GrantedAuthority> authorities = rolePermissionRegistry.getAuthoritiesForRoles(roles);

        UserPrincipal principal = new UserPrincipal(
                userId,
                tenantId,
                email,
                null,
                fullName,
                true,
                roles,
                authorities
        );

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }
}
