package com.enterprise.spendsync.core.internal.service;

import com.enterprise.spendsync.core.internal.dto.AuthTokenResponse;
import com.enterprise.spendsync.core.internal.dto.LoginRequest;
import com.enterprise.spendsync.core.internal.dto.RefreshTokenRequest;

public interface AuthService {

    AuthTokenResponse login(LoginRequest request);

    AuthTokenResponse refreshToken(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);
}
