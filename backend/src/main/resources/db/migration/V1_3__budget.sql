-- =========================================================================
-- SpendSync Migration: V1_3__budget.sql
-- Budget Pools & Fiscal Transaction Ledger
-- =========================================================================

CREATE TABLE IF NOT EXISTS budget_pools (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    cost_center_id UUID NOT NULL REFERENCES cost_centers(id),
    fiscal_year INT NOT NULL,
    period_type VARCHAR(20) NOT NULL,
    period_value VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    enforcement_mode VARCHAR(20) NOT NULL DEFAULT 'HARD_STOP',
    tolerance_percentage NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    allocated_amount NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    spent_amount NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    reserved_amount NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uq_budget_pool_unique_period UNIQUE (tenant_id, cost_center_id, fiscal_year, period_type, period_value)
);

CREATE TABLE IF NOT EXISTS budget_transactions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    budget_pool_id UUID NOT NULL REFERENCES budget_pools(id),
    transaction_type VARCHAR(50) NOT NULL,
    amount NUMERIC(18, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reference_type VARCHAR(50) NOT NULL,
    reference_id UUID NOT NULL,
    description TEXT,
    created_by_user_id UUID REFERENCES users(id),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
