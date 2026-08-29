package com.enterprise.spendsync.notification.internal.repository;

import com.enterprise.spendsync.notification.internal.domain.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    Optional<NotificationPreference> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    Optional<NotificationPreference> findByUserId(UUID userId);
}
