package com.enterprise.spendsync.notification.service;

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
import com.enterprise.spendsync.notification.internal.service.EmailService;
import com.enterprise.spendsync.notification.internal.service.NotificationPreferenceService;
import com.enterprise.spendsync.notification.internal.service.NotificationServiceImpl;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationPreferenceRepository preferenceRepository;
    @Mock
    private NotificationPreferenceService preferenceService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UUID tenantId;
    private UUID recipientId;
    private User recipient;
    private Tenant tenant;
    private NotificationPreference preference;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        recipientId = UUID.randomUUID();

        tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn(tenantId);

        recipient = mock(User.class);
        lenient().when(recipient.getId()).thenReturn(recipientId);
        lenient().when(recipient.getEmail()).thenReturn("approver@enterprise.com");
        lenient().when(recipient.getFirstName()).thenReturn("Jane");
        lenient().when(recipient.getLastName()).thenReturn("Approver");

        preference = new NotificationPreference(recipient, tenant, true, true, null);
    }

    @Nested
    @DisplayName("Dispatch Notification Tests")
    class DispatchNotificationTests {

        @Test
        @DisplayName("Should dispatch both in-app notification and email when preferences allow both")
        void shouldDispatchBothInAppAndEmail() {
            when(userRepository.findByIdAndTenantId(recipientId, tenantId)).thenReturn(Optional.of(recipient));
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(preferenceService.getOrCreatePreferenceEntity(recipientId, tenantId)).thenReturn(preference);

            UUID prId = UUID.randomUUID();
            Map<String, Object> model = Map.of("prNumber", "PR-2026-0001");

            notificationService.dispatchNotification(
                    tenantId,
                    recipientId,
                    NotificationEventType.PR_APPROVAL_REQUESTED,
                    "PR Approval Required",
                    "Please approve PR-2026-0001",
                    NotificationReferenceType.REQUISITION,
                    prId,
                    "pr-approval-request",
                    model
            );

            ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository, times(1)).save(notifCaptor.capture());

            Notification saved = notifCaptor.getValue();
            assertThat(saved.getTitle()).isEqualTo("PR Approval Required");
            assertThat(saved.getEventType()).isEqualTo(NotificationEventType.PR_APPROVAL_REQUESTED);
            assertThat(saved.getReferenceId()).isEqualTo(prId);
            assertThat(saved.isRead()).isFalse();

            verify(emailService, times(1)).sendTemplatedEmail(
                    eq("approver@enterprise.com"),
                    eq("PR Approval Required"),
                    eq("pr-approval-request"),
                    eq(model)
            );
        }

        @Test
        @DisplayName("Should only deliver in-app notification when email is disabled in preferences")
        void shouldOnlyDeliverInAppWhenEmailDisabled() {
            preference.setEmailEnabled(false);

            when(userRepository.findByIdAndTenantId(recipientId, tenantId)).thenReturn(Optional.of(recipient));
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(preferenceService.getOrCreatePreferenceEntity(recipientId, tenantId)).thenReturn(preference);

            notificationService.dispatchNotification(
                    tenantId,
                    recipientId,
                    NotificationEventType.REQUISITION_APPROVED,
                    "PR Approved",
                    "Your PR has been approved",
                    NotificationReferenceType.REQUISITION,
                    UUID.randomUUID(),
                    "pr-decision",
                    Map.of()
            );

            verify(notificationRepository, times(1)).save(any(Notification.class));
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("Should only deliver email when in-app is disabled in preferences")
        void shouldOnlyDeliverEmailWhenInAppDisabled() {
            preference.setInAppEnabled(false);

            when(userRepository.findByIdAndTenantId(recipientId, tenantId)).thenReturn(Optional.of(recipient));
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(preferenceService.getOrCreatePreferenceEntity(recipientId, tenantId)).thenReturn(preference);

            notificationService.dispatchNotification(
                    tenantId,
                    recipientId,
                    NotificationEventType.REQUISITION_APPROVED,
                    "PR Approved",
                    "Your PR has been approved",
                    NotificationReferenceType.REQUISITION,
                    UUID.randomUUID(),
                    "pr-decision",
                    Map.of()
            );

            verify(notificationRepository, never()).save(any(Notification.class));
            verify(emailService, times(1)).sendTemplatedEmail(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should use overridden email address when specified in preferences")
        void shouldUseOverriddenEmailAddress() {
            preference.setEmailAddress("custom.work@enterprise.com");

            when(userRepository.findByIdAndTenantId(recipientId, tenantId)).thenReturn(Optional.of(recipient));
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(preferenceService.getOrCreatePreferenceEntity(recipientId, tenantId)).thenReturn(preference);

            notificationService.dispatchNotification(
                    tenantId,
                    recipientId,
                    NotificationEventType.PR_APPROVAL_REQUESTED,
                    "PR Approval Required",
                    "Body",
                    NotificationReferenceType.REQUISITION,
                    UUID.randomUUID(),
                    null,
                    null
            );

            verify(emailService, times(1)).sendSimpleEmail(eq("custom.work@enterprise.com"), eq("PR Approval Required"), eq("Body"));
        }

        @Test
        @DisplayName("Should abort gracefully if recipient user or tenant is not found")
        void shouldAbortGracefullyIfRecipientNotFound() {
            when(userRepository.findByIdAndTenantId(recipientId, tenantId)).thenReturn(Optional.empty());

            notificationService.dispatchNotification(
                    tenantId,
                    recipientId,
                    NotificationEventType.PR_APPROVAL_REQUESTED,
                    "Title",
                    "Body",
                    null,
                    null,
                    null,
                    null
            );

            verifyNoInteractions(notificationRepository, emailService);
        }
    }

    @Nested
    @DisplayName("In-App Feed & Reading Operations")
    class FeedAndReadingOperations {

        @Test
        @DisplayName("Should fetch paginated notifications for recipient")
        void shouldFetchPaginatedNotifications() {
            Notification notif = new Notification(tenant, recipient, NotificationEventType.PR_APPROVAL_REQUESTED, "Title", "Body", NotificationReferenceType.REQUISITION, UUID.randomUUID());
            Pageable pageable = PageRequest.of(0, 10);
            when(notificationRepository.findAllByTenantIdAndRecipientIdOrderByCreatedAtDesc(tenantId, recipientId, pageable))
                    .thenReturn(new PageImpl<>(List.of(notif)));

            Page<NotificationResponse> result = notificationService.getMyNotifications(tenantId, recipientId, false, pageable);

            assertThat(result).hasSize(1);
            assertThat(result.getContent().get(0).title()).isEqualTo("Title");
            assertThat(result.getContent().get(0).eventType()).isEqualTo("PR_APPROVAL_REQUESTED");
        }

        @Test
        @DisplayName("Should return accurate unread count")
        void shouldReturnAccurateUnreadCount() {
            when(notificationRepository.countByTenantIdAndRecipientIdAndIsReadFalse(tenantId, recipientId)).thenReturn(5L);

            long count = notificationService.getUnreadCount(tenantId, recipientId);

            assertThat(count).isEqualTo(5L);
        }

        @Test
        @DisplayName("Should successfully mark notification as read for recipient")
        void shouldMarkNotificationAsRead() {
            UUID notifId = UUID.randomUUID();
            Notification notif = new Notification(tenant, recipient, NotificationEventType.PR_APPROVAL_REQUESTED, "Title", "Body", null, null);
            when(notificationRepository.findByIdAndTenantId(notifId, tenantId)).thenReturn(Optional.of(notif));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            NotificationResponse response = notificationService.markAsRead(notifId, tenantId, recipientId);

            assertThat(response.isRead()).isTrue();
            assertThat(response.readAt()).isNotNull();
            verify(notificationRepository).save(notif);
        }

        @Test
        @DisplayName("Should throw FORBIDDEN when user attempts to mark another user's notification as read (IDOR Protection)")
        void shouldThrowForbiddenOnIdorAttack() {
            UUID notifId = UUID.randomUUID();
            User otherUser = mock(User.class);
            when(otherUser.getId()).thenReturn(UUID.randomUUID());

            Notification notif = new Notification(tenant, otherUser, NotificationEventType.PR_APPROVAL_REQUESTED, "Title", "Body", null, null);
            when(notificationRepository.findByIdAndTenantId(notifId, tenantId)).thenReturn(Optional.of(notif));

            assertThatThrownBy(() -> notificationService.markAsRead(notifId, tenantId, recipientId))
                    .isInstanceOf(SpendSyncException.class)
                    .satisfies(ex -> assertThat(((SpendSyncException) ex).getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN));

            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should mark all unread notifications as read in bulk")
        void shouldMarkAllUnreadAsReadInBulk() {
            when(notificationRepository.markAllAsReadForRecipient(eq(tenantId), eq(recipientId), any())).thenReturn(7);

            int count = notificationService.markAllAsRead(tenantId, recipientId);

            assertThat(count).isEqualTo(7);
            verify(notificationRepository).markAllAsReadForRecipient(eq(tenantId), eq(recipientId), any());
        }

        @Test
        @DisplayName("Should cleanup old read notifications older than retention days")
        void shouldCleanupOldReadNotifications() {
            when(notificationRepository.deleteOldReadNotifications(any())).thenReturn(25);

            int deleted = notificationService.cleanupOldReadNotifications(90);

            assertThat(deleted).isEqualTo(25);
            verify(notificationRepository).deleteOldReadNotifications(any());
        }
    }
}
