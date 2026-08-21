package com.enterprise.spendsync.shared.security;

import com.enterprise.spendsync.core.internal.domain.RolePermissionRegistry;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtTokenProvider Pure Unit Tests (HMAC-SHA256, Expiry & Claims)")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private RolePermissionRegistry rolePermissionRegistry;

    // 256-bit test secret
    private static final String BASE64_SECRET = Base64.getEncoder().encodeToString(
            "super-secret-spend-sync-enterprise-p2p-jwt-key-256-bits-minimum".getBytes()
    );

    @BeforeEach
    void setUp() {
        rolePermissionRegistry = new RolePermissionRegistry();
        jwtTokenProvider = new JwtTokenProvider(
                BASE64_SECRET,
                900L, // 15 mins
                7L,   // 7 days
                rolePermissionRegistry
        );
    }

    @Test
    @DisplayName("Should generate valid JWT access token with all required claims")
    void shouldGenerateValidAccessToken() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Acme Corporation");

        User user = new User();
        user.setId(userId);
        user.setEmail("finance.cfo@acme.com");
        user.setFirstName("Ahmet");
        user.setLastName("Yılmaz");
        user.setTenant(tenant);
        user.setRoles(Set.of(RoleType.ACCOUNT_USER, RoleType.APPROVER));

        String token = jwtTokenProvider.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();

        Claims claims = jwtTokenProvider.extractClaims(token);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo("finance.cfo@acme.com");
        assertThat(claims.get("tenantId", String.class)).isEqualTo(tenantId.toString());

        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        assertThat(roles).containsExactlyInAnyOrder("ACCOUNT_USER", "APPROVER");
    }

    @Test
    @DisplayName("Should build Spring Authentication object directly from valid token claims")
    void shouldBuildAuthenticationFromToken() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        User user = new User();
        user.setId(userId);
        user.setEmail("procurement@acme.com");
        user.setFirstName("Mehmet");
        user.setLastName("Kaya");
        user.setTenant(tenant);
        user.setRoles(Set.of(RoleType.PROCUREMENT));

        String token = jwtTokenProvider.generateAccessToken(user);

        Authentication auth = jwtTokenProvider.getAuthentication(token);

        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(UserPrincipal.class);

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertThat(principal.getId()).isEqualTo(userId);
        assertThat(principal.getTenantId()).isEqualTo(tenantId);
        assertThat(principal.getEmail()).isEqualTo("procurement@acme.com");
        assertThat(principal.getRoles()).contains(RoleType.PROCUREMENT);
        // PROCUREMENT must have PERM_PO_CREATE authority
        assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("PERM_PO_CREATE"));
    }

    @Test
    @DisplayName("Should reject tampered or modified JWT token")
    void shouldRejectTamperedToken() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@acme.com");
        user.setRoles(Set.of(RoleType.ACCOUNT_USER));

        String token = jwtTokenProvider.generateAccessToken(user);
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertThat(jwtTokenProvider.validateToken(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("Should reject expired JWT token")
    void shouldRejectExpiredToken() {
        // JwtTokenProvider with 0 second expiry
        JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(
                BASE64_SECRET,
                0L,
                7L,
                rolePermissionRegistry
        );

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("expired@acme.com");
        user.setRoles(Set.of(RoleType.ACCOUNT_USER));

        String token = expiredTokenProvider.generateAccessToken(user);

        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("Should generate 64-hex char secure random refresh token string")
    void shouldGenerateSecureRefreshTokenString() {
        String refreshToken = jwtTokenProvider.generateRefreshTokenString();

        assertThat(refreshToken).isNotBlank().hasSize(64);
        assertThat(refreshToken).matches("^[0-9a-f]{64}$");
    }
}
