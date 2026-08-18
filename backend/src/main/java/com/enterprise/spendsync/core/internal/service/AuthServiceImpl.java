package com.enterprise.spendsync.core.internal.service;

import com.enterprise.spendsync.core.internal.domain.RefreshToken;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.dto.AuthTokenResponse;
import com.enterprise.spendsync.core.internal.dto.LoginRequest;
import com.enterprise.spendsync.core.internal.dto.RefreshTokenRequest;
import com.enterprise.spendsync.core.internal.repository.RefreshTokenRepository;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public AuthTokenResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new SpendSyncException("Invalid email or password", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS") {});

        if (!user.isActive()) {
            throw new SpendSyncException("User account has been suspended or deactivated", HttpStatus.FORBIDDEN, "ACCOUNT_DEACTIVATED") {};
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new SpendSyncException("Invalid email or password", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS") {};
        }

        // Update login audit timestamp
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // Generate Access Token
        String accessToken = jwtTokenProvider.generateAccessToken(user);

        // Generate Refresh Token string and save in DB
        String refreshTokenString = jwtTokenProvider.generateRefreshTokenString();
        Instant refreshExpiry = jwtTokenProvider.calculateRefreshTokenExpiry();

        RefreshToken refreshToken = new RefreshToken(
                user,
                user.getTenant(),
                refreshTokenString,
                refreshExpiry
        );
        refreshTokenRepository.save(refreshToken);

        UUID tenantId = user.getTenant() != null ? user.getTenant().getId() : null;
        String fullName = user.getFirstName() + " " + user.getLastName();
        Set<String> roleNames = user.getRoles().stream().map(RoleType::name).collect(Collectors.toSet());

        return AuthTokenResponse.of(
                accessToken,
                refreshTokenString,
                jwtTokenProvider.getAccessTokenExpirySeconds(),
                user.getId(),
                user.getEmail(),
                fullName,
                tenantId,
                roleNames
        );
    }

    @Override
    public AuthTokenResponse refreshToken(RefreshTokenRequest request) {
        if (request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new SpendSyncException("Refresh token is required", HttpStatus.BAD_REQUEST, "MISSING_REFRESH_TOKEN") {};
        }

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(request.refreshToken().trim())
                .orElseThrow(() -> new SpendSyncException("Invalid or unrecognized refresh token", HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN") {});

        if (!refreshToken.isValid()) {
            throw new SpendSyncException("Refresh token has expired or been revoked", HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED_OR_REVOKED") {};
        }

        User user = refreshToken.getUser();
        if (!user.isActive()) {
            throw new SpendSyncException("User account is inactive", HttpStatus.FORBIDDEN, "ACCOUNT_DEACTIVATED") {};
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        UUID tenantId = user.getTenant() != null ? user.getTenant().getId() : null;
        String fullName = user.getFirstName() + " " + user.getLastName();
        Set<String> roleNames = user.getRoles().stream().map(RoleType::name).collect(Collectors.toSet());

        return AuthTokenResponse.of(
                newAccessToken,
                refreshToken.getTokenHash(),
                jwtTokenProvider.getAccessTokenExpirySeconds(),
                user.getId(),
                user.getEmail(),
                fullName,
                tenantId,
                roleNames
        );
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            refreshTokenRepository.revokeByTokenHash(request.refreshToken().trim());
        }
    }
}
