-- =========================================================================
-- SpendSync Migration: V1_11__audit_and_indexes.sql
-- SOX-404 Immutable Audit Ledger & High-Performance Multi-Tenant Indexes
-- =========================================================================

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    correlation_id VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    compliance_tag VARCHAR(100) NOT NULL,
    actor_id UUID,
    actor_email VARCHAR(255),
    actor_role VARCHAR(100),
    ip_address VARCHAR(50),
    user_agent TEXT,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    legal_entity_id UUID,
    cost_center_id UUID,
    amount NUMERIC(18, 4),
    currency VARCHAR(10),
    from_status VARCHAR(50),
    to_status VARCHAR(50),
    decision_note TEXT,
    payload TEXT,
    checksum VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

-- High-Performance Multi-Tenant Indexes
CREATE INDEX IF NOT EXISTS idx_users_tenant_email ON users(tenant_id, email);
CREATE INDEX IF NOT EXISTS idx_vendors_tenant_tax ON vendors(tenant_id, tax_number);
CREATE INDEX IF NOT EXISTS idx_catalog_tenant_code ON catalog_items(tenant_id, item_code);
CREATE INDEX IF NOT EXISTS idx_catalog_category_parent ON catalog_categories(tenant_id, parent_id);
CREATE INDEX IF NOT EXISTS idx_budget_pool_lookup ON budget_pools(tenant_id, cost_center_id, fiscal_year);
CREATE INDEX IF NOT EXISTS idx_requisitions_tenant_status ON purchase_requisitions(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_tenant_status ON purchase_orders(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_invoices_tenant_status ON supplier_invoices(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_created ON audit_logs(tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_correlation ON audit_logs(correlation_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON audit_logs(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_compliance ON audit_logs(compliance_tag);
