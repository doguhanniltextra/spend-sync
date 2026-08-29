-- ============================================================================
-- V1_12__notifications.sql: Enterprise Notification Engine & User Preferences
-- ============================================================================

-- 1. In-App Notification Feed
CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    recipient_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_type      VARCHAR(80) NOT NULL,
    title           VARCHAR(200) NOT NULL,
    body            TEXT NOT NULL,
    reference_type  VARCHAR(50),
    reference_id    UUID,
    is_read         BOOLEAN DEFAULT FALSE NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    read_at         TIMESTAMPTZ
);

-- Performance index for real-time user notification badge & inbox pagination
CREATE INDEX idx_notifications_recipient_unread 
    ON notifications(tenant_id, recipient_id, is_read, created_at DESC);

CREATE INDEX idx_notifications_cleanup 
    ON notifications(created_at, is_read);

-- 2. User Notification Channel Preferences
CREATE TABLE notification_preferences (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    email_enabled   BOOLEAN DEFAULT TRUE NOT NULL,
    in_app_enabled  BOOLEAN DEFAULT TRUE NOT NULL,
    email_address   VARCHAR(255),
    created_at      TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at      TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_notif_pref_user ON notification_preferences(tenant_id, user_id);
