package com.enterprise.spendsync.notification.internal.web;

import com.enterprise.spendsync.notification.api.dto.NotificationPreferenceRequest;
import com.enterprise.spendsync.notification.api.dto.NotificationPreferenceResponse;
import com.enterprise.spendsync.notification.api.dto.NotificationResponse;
import com.enterprise.spendsync.notification.api.dto.UnreadNotificationCountResponse;
import com.enterprise.spendsync.notification.internal.service.NotificationPreferenceService;
import com.enterprise.spendsync.notification.internal.service.NotificationService;
import com.enterprise.spendsync.shared.config.Endpoints;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.security.UserPrincipal;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(Endpoints.Notification.BASE)
@Tag(name = "Notification Engine", description = "In-App Notifications Feed & User Preference Management")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;

    public NotificationController(NotificationService notificationService,
                                  NotificationPreferenceService preferenceService) {
        this.notificationService = notificationService;
        this.preferenceService = preferenceService;
    }

    @GetMapping
    @Operation(summary = "Get current user notifications", description = "Returns a paginated list of in-app notifications for the authenticated user.")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @RequestParam(name = "unread", required = false) Boolean unreadOnly,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        UUID userId = getCurrentUserId();
        Page<NotificationResponse> page = notificationService.getMyNotifications(tenantId, userId, unreadOnly, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping(Endpoints.Notification.UNREAD_COUNT)
    @Operation(summary = "Get unread notification count", description = "Returns the number of unread notifications for badge display.")
    public ResponseEntity<UnreadNotificationCountResponse> getUnreadCount() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        UUID userId = getCurrentUserId();
        long count = notificationService.getUnreadCount(tenantId, userId);
        return ResponseEntity.ok(new UnreadNotificationCountResponse(count));
    }

    @PatchMapping(Endpoints.Notification.MARK_READ)
    @Operation(summary = "Mark single notification as read", description = "Updates a notification's status to read.")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable("id") UUID id) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        UUID userId = getCurrentUserId();
        NotificationResponse response = notificationService.markAsRead(id, tenantId, userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping(Endpoints.Notification.MARK_ALL_READ)
    @Operation(summary = "Mark all notifications as read", description = "Bulk marks all unread notifications of the current user as read.")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        UUID userId = getCurrentUserId();
        int updated = notificationService.markAllAsRead(tenantId, userId);
        return ResponseEntity.ok(Map.of("markedReadCount", updated, "success", true));
    }

    @GetMapping(Endpoints.Notification.PREFERENCES)
    @Operation(summary = "Get notification preferences", description = "Retrieves user channel delivery preferences.")
    public ResponseEntity<NotificationPreferenceResponse> getPreferences() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        UUID userId = getCurrentUserId();
        NotificationPreferenceResponse response = preferenceService.getPreferences(userId, tenantId);
        return ResponseEntity.ok(response);
    }

    @PutMapping(Endpoints.Notification.PREFERENCES)
    @Operation(summary = "Update notification preferences", description = "Updates user channel delivery preferences.")
    public ResponseEntity<NotificationPreferenceResponse> updatePreferences(@Valid @RequestBody NotificationPreferenceRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        UUID userId = getCurrentUserId();
        NotificationPreferenceResponse response = preferenceService.updatePreferences(userId, tenantId, request);
        return ResponseEntity.ok(response);
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SpendSyncException("Authentication required", HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED") {};
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserPrincipal up) {
            return up.getId();
        }
        throw new SpendSyncException("Invalid user principal in security context", HttpStatus.UNAUTHORIZED, "INVALID_PRINCIPAL") {};
    }
}
