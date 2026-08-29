package com.enterprise.spendsync.notification.web;

import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.notification.api.dto.NotificationPreferenceRequest;
import com.enterprise.spendsync.notification.api.dto.NotificationPreferenceResponse;
import com.enterprise.spendsync.notification.api.dto.NotificationResponse;
import com.enterprise.spendsync.notification.internal.service.NotificationPreferenceService;
import com.enterprise.spendsync.notification.internal.service.NotificationService;
import com.enterprise.spendsync.notification.internal.web.NotificationController;
import com.enterprise.spendsync.shared.config.Endpoints;
import com.enterprise.spendsync.shared.security.UserPrincipal;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController Standalone Unit / API Tests")
class NotificationControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationPreferenceService preferenceService;

    @InjectMocks
    private NotificationController notificationController;

    private UUID tenantId;
    private UUID userId;

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"pageable"})
    private abstract static class PageMixIn {}

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .addMixIn(PageImpl.class, PageMixIn.class);

        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(
                        new org.springframework.http.converter.ByteArrayHttpMessageConverter(),
                        new org.springframework.http.converter.StringHttpMessageConverter(),
                        new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(mapper)
                )
                .build();

        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();

        TenantContext.setTenantId(tenantId);

        UserPrincipal principal = new UserPrincipal(
                userId,
                tenantId,
                "test@enterprise.com",
                "hash",
                "Test User",
                true,
                Set.of(RoleType.REQUISITIONER),
                List.of()
        );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/notifications should return paginated list of user notifications")
    void shouldReturnNotifications() throws Exception {
        NotificationResponse res = new NotificationResponse(
                UUID.randomUUID(), "PR_APPROVAL_REQUESTED", "PR Approval", "Body text",
                "REQUISITION", UUID.randomUUID(), false, Instant.now(), null
        );

        when(notificationService.getMyNotifications(eq(tenantId), eq(userId), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(res)));

        mockMvc.perform(get(Endpoints.Notification.BASE)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("PR Approval"))
                .andExpect(jsonPath("$.content[0].eventType").value("PR_APPROVAL_REQUESTED"));
    }

    @Test
    @DisplayName("GET /api/v1/notifications/count/unread should return unread badge count")
    void shouldReturnUnreadCount() throws Exception {
        when(notificationService.getUnreadCount(tenantId, userId)).thenReturn(3L);

        mockMvc.perform(get(Endpoints.Notification.BASE + Endpoints.Notification.UNREAD_COUNT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));
    }

    @Test
    @DisplayName("PATCH /api/v1/notifications/{id}/read should mark notification as read")
    void shouldMarkAsRead() throws Exception {
        UUID notifId = UUID.randomUUID();
        NotificationResponse res = new NotificationResponse(
                notifId, "PR_APPROVAL_REQUESTED", "PR Approval", "Body text",
                "REQUISITION", UUID.randomUUID(), true, Instant.now(), Instant.now()
        );

        when(notificationService.markAsRead(notifId, tenantId, userId)).thenReturn(res);

        mockMvc.perform(patch(Endpoints.Notification.BASE + "/" + notifId + "/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    @DisplayName("PATCH /api/v1/notifications/read-all should bulk mark as read")
    void shouldBulkMarkAllAsRead() throws Exception {
        when(notificationService.markAllAsRead(tenantId, userId)).thenReturn(5);

        mockMvc.perform(patch(Endpoints.Notification.BASE + Endpoints.Notification.MARK_ALL_READ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markedReadCount").value(5))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/notifications/preferences should return user preferences")
    void shouldGetPreferences() throws Exception {
        NotificationPreferenceResponse pref = new NotificationPreferenceResponse(
                UUID.randomUUID(), userId, true, true, "user@enterprise.com", Instant.now()
        );

        when(preferenceService.getPreferences(userId, tenantId)).thenReturn(pref);

        mockMvc.perform(get(Endpoints.Notification.BASE + Endpoints.Notification.PREFERENCES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnabled").value(true))
                .andExpect(jsonPath("$.inAppEnabled").value(true));
    }

    @Test
    @DisplayName("PUT /api/v1/notifications/preferences should update preferences")
    void shouldUpdatePreferences() throws Exception {
        NotificationPreferenceRequest req = new NotificationPreferenceRequest(false, true, "alert@enterprise.com");
        NotificationPreferenceResponse res = new NotificationPreferenceResponse(
                UUID.randomUUID(), userId, false, true, "alert@enterprise.com", Instant.now()
        );

        when(preferenceService.updatePreferences(eq(userId), eq(tenantId), any(NotificationPreferenceRequest.class)))
                .thenReturn(res);

        mockMvc.perform(put(Endpoints.Notification.BASE + Endpoints.Notification.PREFERENCES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnabled").value(false))
                .andExpect(jsonPath("$.emailAddress").value("alert@enterprise.com"));
    }
}
