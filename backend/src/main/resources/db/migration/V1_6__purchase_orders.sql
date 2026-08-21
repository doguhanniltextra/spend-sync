-- =========================================================================
-- SpendSync Migration: V1_6__purchase_orders.sql
-- Purchase Orders (PO), Order Lines & Audit Version Revisions
-- =========================================================================

CREATE TABLE IF NOT EXISTS purchase_orders (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    po_number VARCHAR(50) NOT NULL,
    requisition_id UUID REFERENCES purchase_requisitions(id),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    cost_center_id UUID NOT NULL REFERENCES cost_centers(id),
    delivery_facility_id UUID REFERENCES facilities(id),
    vendor_id UUID NOT NULL REFERENCES vendors(id),
    status VARCHAR(50) NOT NULL,
    incoterms VARCHAR(20) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
    payment_terms VARCHAR(50) NOT NULL,
    notes TEXT,
    total_amount NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    created_by_user_id UUID REFERENCES users(id),
    issued_at TIMESTAMP WITHOUT TIME ZONE,
    acknowledged_at TIMESTAMP WITHOUT TIME ZONE,
    fulfilled_at TIMESTAMP WITHOUT TIME ZONE,
    cancelled_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uq_po_tenant_number UNIQUE (tenant_id, po_number)
);

CREATE TABLE IF NOT EXISTS purchase_order_line_items (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    purchase_order_id UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    requisition_line_item_id UUID REFERENCES requisition_line_items(id),
    line_number INT NOT NULL,
    item_description VARCHAR(500) NOT NULL,
    item_category VARCHAR(100) NOT NULL,
    quantity NUMERIC(18, 4) NOT NULL,
    unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'PIECE',
    unit_price NUMERIC(18, 4) NOT NULL,
    line_total_amount NUMERIC(18, 4) NOT NULL,
    over_delivery_tolerance_pct NUMERIC(5, 2),
    under_delivery_tolerance_pct NUMERIC(5, 2),
    estimated_delivery_date DATE
);

CREATE TABLE IF NOT EXISTS purchase_order_revisions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    purchase_order_id UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    revision_number INT NOT NULL,
    change_summary TEXT NOT NULL,
    previous_state_json TEXT NOT NULL,
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
