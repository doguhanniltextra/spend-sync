-- =========================================================================
-- SpendSync Migration: V1_5__requisitions.sql
-- Purchase Requisitions (PR), Line Items & Approval Workflow Steps
-- =========================================================================

CREATE TABLE IF NOT EXISTS purchase_requisitions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    requisition_number VARCHAR(50) NOT NULL,
    requisitioner_id UUID NOT NULL REFERENCES users(id),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    cost_center_id UUID NOT NULL REFERENCES cost_centers(id),
    delivery_facility_id UUID NOT NULL REFERENCES facilities(id),
    budget_pool_id UUID REFERENCES budget_pools(id),
    status VARCHAR(50) NOT NULL,
    total_amount NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
    title VARCHAR(255) NOT NULL,
    justification TEXT,
    approved_at TIMESTAMP WITHOUT TIME ZONE,
    rejected_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uq_requisition_tenant_number UNIQUE (tenant_id, requisition_number)
);

CREATE TABLE IF NOT EXISTS requisition_line_items (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    purchase_requisition_id UUID NOT NULL REFERENCES purchase_requisitions(id) ON DELETE CASCADE,
    line_number INT NOT NULL,
    item_description VARCHAR(500) NOT NULL,
    category_code VARCHAR(100) NOT NULL,
    quantity NUMERIC(18, 4) NOT NULL,
    unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'PIECE',
    estimated_unit_price NUMERIC(18, 4) NOT NULL,
    tax_rate NUMERIC(5, 4) NOT NULL DEFAULT 0.2000,
    estimated_total_amount NUMERIC(18, 4) NOT NULL,
    suggested_vendor_id UUID REFERENCES vendors(id),
    gl_account_code VARCHAR(50),
    catalog_item_id UUID REFERENCES catalog_items(id)
);

CREATE TABLE IF NOT EXISTS requisition_approval_steps (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    purchase_requisition_id UUID NOT NULL REFERENCES purchase_requisitions(id) ON DELETE CASCADE,
    step_order INT NOT NULL,
    approver_user_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(50) NOT NULL,
    decision_notes TEXT,
    decided_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
