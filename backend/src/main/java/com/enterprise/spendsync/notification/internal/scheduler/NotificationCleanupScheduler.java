package com.enterprise.spendsync.notification.internal.scheduler;

import com.enterprise.spendsync.notification.internal.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enterprise Notification Retention Policy Enforcer.
 * Periodically purges historical read notifications older than configured retention threshold (default: 90 days).
 */
@Component
public class NotificationCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationCleanupScheduler.class);

    private final NotificationService notificationService;

    @Value("${spendsync.notification.retention-days:90}")
    private int retentionDays;

    public NotificationCleanupScheduler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "${spendsync.scheduler.cleanup.cron:0 0 2 * * SUN}")
    @Transactional
    public void cleanupOldNotifications() {
        log.info("Executing scheduled notification cleanup for records older than {} days", retentionDays);
        try {
            int deletedCount = notificationService.cleanupOldReadNotifications(retentionDays);
            log.info("Successfully completed notification cleanup. Total removed: {}", deletedCount);
        } catch (Exception ex) {
            log.error("Failed to execute notification cleanup: {}", ex.getMessage(), ex);
        }
    }
}
