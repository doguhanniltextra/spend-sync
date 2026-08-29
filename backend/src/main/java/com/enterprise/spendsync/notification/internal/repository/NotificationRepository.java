package com.enterprise.spendsync.notification.internal.repository;

import com.enterprise.spendsync.notification.internal.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<Notification> findAllByTenantIdAndRecipientIdOrderByCreatedAtDesc(UUID tenantId, UUID recipientId, Pageable pageable);

    Page<Notification> findAllByTenantIdAndRecipientIdAndIsReadOrderByCreatedAtDesc(UUID tenantId, UUID recipientId, boolean isRead, Pageable pageable);

    long countByTenantIdAndRecipientIdAndIsReadFalse(UUID tenantId, UUID recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now WHERE n.tenant.id = :tenantId AND n.recipient.id = :recipientId AND n.isRead = false")
    int markAllAsReadForRecipient(@Param("tenantId") UUID tenantId, @Param("recipientId") UUID recipientId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.createdAt < :cutoffDate")
    int deleteOldReadNotifications(@Param("cutoffDate") Instant cutoffDate);
}
