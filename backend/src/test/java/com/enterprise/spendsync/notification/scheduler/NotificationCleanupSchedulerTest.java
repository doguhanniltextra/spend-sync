package com.enterprise.spendsync.notification.scheduler;

import com.enterprise.spendsync.notification.internal.scheduler.NotificationCleanupScheduler;
import com.enterprise.spendsync.notification.internal.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupSchedulerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationCleanupScheduler cleanupScheduler;

    @Test
    @DisplayName("Should invoke cleanup on notification service with configured retention threshold")
    void shouldInvokeCleanupWithConfiguredRetentionDays() {
        ReflectionTestUtils.setField(cleanupScheduler, "retentionDays", 90);
        when(notificationService.cleanupOldReadNotifications(90)).thenReturn(14);

        cleanupScheduler.cleanupOldNotifications();

        verify(notificationService).cleanupOldReadNotifications(90);
    }
}
