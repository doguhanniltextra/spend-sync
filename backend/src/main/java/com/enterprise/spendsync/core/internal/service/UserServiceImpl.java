package com.enterprise.spendsync.core.internal.service;

import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.dto.RegisterUserRequest;
import com.enterprise.spendsync.core.internal.dto.UserResponse;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.shared.exception.EmailAlreadyExistsException;
import com.enterprise.spendsync.shared.exception.InvalidPasswordException;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    // ISO 27001 Password policy: At least 8 chars, 1 uppercase, 1 lowercase, 1 digit
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse registerUser(RegisterUserRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        // 1. Check uniqueness
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        // 2. Validate password complexity
        validatePassword(request.password());

        // 3. Hash password
        String hashedPassword = passwordEncoder.encode(request.password());

        // 4. Create User entity
        User user = new User(
                normalizedEmail,
                hashedPassword,
                request.firstName().trim(),
                request.lastName().trim(),
                request.phoneNumber() != null ? request.phoneNumber().trim() : null,
                request.country() != null ? request.country().trim().toUpperCase() : "TR"
        );

        if (request.jobTitle() != null && !request.jobTitle().isBlank()) {
            user.setJobTitle(request.jobTitle().trim());
        }
        if (request.timezone() != null && !request.timezone().isBlank()) {
            user.setTimezone(request.timezone().trim());
        }
        if (request.preferredLanguage() != null && !request.preferredLanguage().isBlank()) {
            user.setPreferredLanguage(request.preferredLanguage().trim().toLowerCase());
        }

        // Default role for standalone self-registered initial user
        user.addRole(RoleType.ROOT_USER);

        User savedUser = userRepository.save(user);

        return UserResponse.fromEntity(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new SpendSyncException("User with id '" + id + "' was not found.", HttpStatus.NOT_FOUND, "USER_NOT_FOUND") {});
        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new SpendSyncException("User with email '" + normalizedEmail + "' was not found.", HttpStatus.NOT_FOUND, "USER_NOT_FOUND") {});
        return UserResponse.fromEntity(user);
    }

    private void validatePassword(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new InvalidPasswordException("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, and one number.");
        }
    }
}
