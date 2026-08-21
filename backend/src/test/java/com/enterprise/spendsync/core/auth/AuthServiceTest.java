package com.enterprise.spendsync.core.auth;

import com.enterprise.spendsync.core.internal.domain.RefreshToken;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.dto.AuthTokenResponse;
import com.enterprise.spendsync.core.internal.dto.LoginRequest;
import com.enterprise.spendsync.core.internal.dto.RefreshTokenRequest;
import com.enterprise.spendsync.core.internal.repository.RefreshTokenRepository;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.core.internal.service.AuthServiceImpl;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit & Mock Tests (Login, JWT & Token Lifecycle)")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        testTenant = new Tenant();
        testTenant.setId(UUID.randomUUID());
        testTenant.setName("SpendSync Enterprise A.S.");

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("admin@spendsync.com");
        testUser.setPasswordHash("$2a$10$hashedPassword123");
        testUser.setFirstName("Doguhan");
        testUser.setLastName("Kose");
        testUser.setActive(true);
        testUser.setTenant(testTenant);
        testUser.setRoles(Set.of(RoleType.ROOT_USER));
    }

    @Test
    @DisplayName("Should successfully authenticate user with correct credentials and issue tokens")
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest("admin@spendsync.com", "Secret123!");

        when(userRepository.findByEmail("admin@spendsync.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Secret123!", testUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("mocked.access.jwt.token");
        when(jwtTokenProvider.generateRefreshTokenString()).thenReturn("mocked-random-refresh-token-64-hex");
        when(jwtTokenProvider.calculateRefreshTokenExpiry()).thenReturn(Instant.now().plusSeconds(604800));
        when(jwtTokenProvider.getAccessTokenExpirySeconds()).thenReturn(900L);

        AuthTokenResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("mocked.access.jwt.token");
        assertThat(response.refreshToken()).isEqualTo("mocked-random-refresh-token-64-hex");
        assertThat(response.email()).isEqualTo("admin@spendsync.com");
        assertThat(response.roles()).contains("ROOT_USER");

        verify(userRepository).save(testUser);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should throw 401 when email is not found")
    void shouldThrowWhenEmailNotFound() {
        LoginRequest request = new LoginRequest("unknown@spendsync.com", "Password123!");

        when(userRepository.findByEmail("unknown@spendsync.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("Invalid email or password");

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("Should throw 401 when password does not match")
    void shouldThrowWhenPasswordMismatch() {
        LoginRequest request = new LoginRequest("admin@spendsync.com", "WrongPassword!");

        when(userRepository.findByEmail("admin@spendsync.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPassword!", testUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("Invalid email or password");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw 403 when user account is deactivated/suspended")
    void shouldThrowWhenAccountDeactivated() {
        testUser.setActive(false);
        LoginRequest request = new LoginRequest("admin@spendsync.com", "Secret123!");

        when(userRepository.findByEmail("admin@spendsync.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("User account has been suspended or deactivated");

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("Should successfully refresh access token using valid refresh token")
    void shouldRefreshTokenSuccessfully() {
        RefreshToken refreshToken = new RefreshToken(
                testUser,
                testTenant,
                "valid-token-hash",
                Instant.now().plusSeconds(3600)
        );

        when(refreshTokenRepository.findByTokenHash("valid-token-hash")).thenReturn(Optional.of(refreshToken));
        when(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("new.refreshed.access.token");
        when(jwtTokenProvider.getAccessTokenExpirySeconds()).thenReturn(900L);

        AuthTokenResponse response = authService.refreshToken(new RefreshTokenRequest("valid-token-hash"));

        assertThat(response.accessToken()).isEqualTo("new.refreshed.access.token");
        assertThat(response.refreshToken()).isEqualTo("valid-token-hash");
    }

    @Test
    @DisplayName("Should revoke token on logout")
    void shouldRevokeTokenOnLogout() {
        authService.logout(new RefreshTokenRequest("token-to-revoke"));

        verify(refreshTokenRepository).revokeByTokenHash("token-to-revoke");
    }
}
