package com.enterprise.spendsync.core.internal.dto;

import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.domain.User;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Public response DTO representing a sanitized user account.
 */
public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String fullName,
        String jobTitle,
        String phoneNumber,
        String country,
        String timezone,
        String preferredLanguage,
        boolean isActive,
        boolean isEmailVerified,
        Set<RoleType> roles,
        Instant createdAt
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(),
                user.getJobTitle(),
                user.getPhoneNumber(),
                user.getCountry(),
                user.getTimezone(),
                user.getPreferredLanguage(),
                user.isActive(),
                user.isEmailVerified(),
                user.getRoles(),
                user.getCreatedAt()
        );
    }
}
