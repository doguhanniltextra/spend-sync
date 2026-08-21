-- =========================================================================
-- SpendSync Migration: V1_4__approval_limits.sql
-- Delegation of Authority (DoA) Financial Approval Matrix
-- =========================================================================

CREATE TABLE IF NOT EXISTS approval_authority_limits (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID NOT NULL REFERENCES users(id),
    legal_entity_id UUID REFERENCES legal_entities(id),
    cost_center_id UUID REFERENCES cost_centers(id),
    approval_level INT NOT NULL,
    min_amount NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    max_amount NUMERIC(18, 4),
    currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
