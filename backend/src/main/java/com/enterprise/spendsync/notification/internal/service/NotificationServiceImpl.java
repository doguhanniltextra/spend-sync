package com.enterprise.spendsync.notification.internal.service;

import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.notification.api.dto.NotificationResponse;
import com.enterprise.spendsync.notification.internal.domain.Notification;
import com.enterprise.spendsync.notification.internal.domain.NotificationEventType;
import com.enterprise.spendsync.notification.internal.domain.NotificationPreference;
import com.enterprise.spendsync.notification.internal.domain.NotificationReferenceType;
import com.enterprise.spendsync.notification.internal.repository.NotificationPreferenceRepository;
import com.enterprise.spendsync.notification.internal.repository.NotificationRepository;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationPreferenceService preferenceService;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final EmailService emailService;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   NotificationPreferenceRepository preferenceRepository,
                                   NotificationPreferenceService preferenceService,
                                   UserRepository userRepository,
                                   TenantRepository tenantRepository,
                                   EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.preferenceService = preferenceService;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.emailService = emailService;
    }

    @Override
    public void dispatchNotification(
            UUID tenantId,
            UUID recipientId,
            NotificationEventType eventType,
            String title,
            String body,
            NotificationReferenceType referenceType,
            UUID referenceId,
            String emailTemplateName,
            Map<String, Object> templateModel
    ) {
        User recipient = userRepository.findByIdAndTenantId(recipientId, tenantId)
                .orElse(null);

        if (recipient == null) {
            log.warn("Recipient user {} not found in tenant {}. Notification dispatch aborted.", recipientId, tenantId);
            return;
        }

        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            log.warn("Tenant {} not found. Notification dispatch aborted.", tenantId);
            return;
        }

        NotificationPreference preference = preferenceService.getOrCreatePreferenceEntity(recipientId, tenantId);

        // 1. In-App Notification Delivery
        if (preference.isInAppEnabled()) {
            Notification notification = new Notification(
                    tenant,
                    recipient,
                    eventType,
                    title,
                    body,
                    referenceType,
                    referenceId
            );
            notificationRepository.save(notification);
            log.debug("Saved in-app notification for user {} on event {}", recipientId, eventType);
        } else {
            log.debug("In-app notifications disabled in preferences for user {}", recipientId);
        }

        // 2. Email Delivery
        if (preference.isEmailEnabled()) {
            String targetEmail = preference.getEmailAddress() != null && !preference.getEmailAddress().isBlank()
                    ? preference.getEmailAddress()
                    : recipient.getEmail();

            if (targetEmail != null && !targetEmail.isBlank()) {
                if (emailTemplateName != null && !emailTemplateName.isBlank()) {
                    emailService.sendTemplatedEmail(targetEmail, title, emailTemplateName, templateModel);
                } else {
                    emailService.sendSimpleEmail(targetEmail, title, body);
                }
            } else {
                log.warn("No valid email address found for recipient user {}", recipientId);
            }
        } else {
            log.debug("Email notifications disabled in preferences for user {}", recipientId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(UUID tenantId, UUID recipientId, Boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = (unreadOnly != null && unreadOnly)
                ? notificationRepository.findAllByTenantIdAndRecipientIdAndIsReadOrderByCreatedAtDesc(tenantId, recipientId, false, pageable)
                : notificationRepository.findAllByTenantIdAndRecipientIdOrderByCreatedAtDesc(tenantId, recipientId, pageable);

        return page.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID tenantId, UUID recipientId) {
        return notificationRepository.countByTenantIdAndRecipientIdAndIsReadFalse(tenantId, recipientId);
    }

    @Override
    public NotificationResponse markAsRead(UUID notificationId, UUID tenantId, UUID recipientId) {
        Notification notification = notificationRepository.findByIdAndTenantId(notificationId, tenantId)
                .orElseThrow(() -> new SpendSyncException("Notification not found", HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND") {});

        if (!notification.getRecipient().getId().equals(recipientId)) {
            throw new SpendSyncException("You do not have permission to view or modify this notification", HttpStatus.FORBIDDEN, "FORBIDDEN") {};
        }

        notification.markAsRead();
        Notification saved = notificationRepository.save(notification);
        return mapToResponse(saved);
    }

    @Override
    public int markAllAsRead(UUID tenantId, UUID recipientId) {
        return notificationRepository.markAllAsReadForRecipient(tenantId, recipientId, Instant.now());
    }

    @Override
    public int cleanupOldReadNotifications(int daysOld) {
        Instant cutoff = Instant.now().minus(daysOld, ChronoUnit.DAYS);
        int deleted = notificationRepository.deleteOldReadNotifications(cutoff);
        log.info("Cleaned up {} old read notifications older than {} days (cutoff: {})", deleted, daysOld, cutoff);
        return deleted;
    }

    private NotificationResponse mapToResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getEventType().name(),
                n.getTitle(),
                n.getBody(),
                n.getReferenceType() != null ? n.getReferenceType().name() : null,
                n.getReferenceId(),
                n.isRead(),
                n.getCreatedAt(),
                n.getReadAt()
        );
    }
}
