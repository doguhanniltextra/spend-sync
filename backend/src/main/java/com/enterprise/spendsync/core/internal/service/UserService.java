package com.enterprise.spendsync.core.internal.service;

import com.enterprise.spendsync.core.internal.dto.RegisterUserRequest;
import com.enterprise.spendsync.core.internal.dto.UserResponse;

import java.util.UUID;

public interface UserService {
    UserResponse registerUser(RegisterUserRequest request);
    UserResponse getUserById(UUID id);
    UserResponse getUserByEmail(String email);
}
