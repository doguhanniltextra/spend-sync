package com.enterprise.spendsync.notification.internal.service;

import com.enterprise.spendsync.notification.api.dto.NotificationResponse;
import com.enterprise.spendsync.notification.internal.domain.NotificationEventType;
import com.enterprise.spendsync.notification.internal.domain.NotificationReferenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface NotificationService {

    void dispatchNotification(
            UUID tenantId,
            UUID recipientId,
            NotificationEventType eventType,
            String title,
            String body,
            NotificationReferenceType referenceType,
            UUID referenceId,
            String emailTemplateName,
            Map<String, Object> templateModel
    );

    Page<NotificationResponse> getMyNotifications(UUID tenantId, UUID recipientId, Boolean unreadOnly, Pageable pageable);

    long getUnreadCount(UUID tenantId, UUID recipientId);

    NotificationResponse markAsRead(UUID notificationId, UUID tenantId, UUID recipientId);

    int markAllAsRead(UUID tenantId, UUID recipientId);

    int cleanupOldReadNotifications(int daysOld);
}
