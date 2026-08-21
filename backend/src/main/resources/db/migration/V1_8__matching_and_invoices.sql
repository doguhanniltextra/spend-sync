-- =========================================================================
-- SpendSync Migration: V1_8__matching_and_invoices.sql
-- Supplier Invoices, UBL-TR XML, Tevkifat Math & Discrepancies
-- =========================================================================

CREATE TABLE IF NOT EXISTS supplier_invoices (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    invoice_number VARCHAR(50) NOT NULL,
    ettn VARCHAR(100) NOT NULL,
    invoice_date DATE NOT NULL,
    due_date DATE,
    invoice_type VARCHAR(50) NOT NULL,
    invoice_profile VARCHAR(50) NOT NULL,
    purchase_order_id UUID REFERENCES purchase_orders(id),
    vendor_id UUID NOT NULL REFERENCES vendors(id),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    cost_center_id UUID NOT NULL REFERENCES cost_centers(id),
    currency VARCHAR(10) NOT NULL DEFAULT 'TRY',
    exchange_rate NUMERIC(15, 6) DEFAULT 1.000000,
    subtotal_amount NUMERIC(18, 4) NOT NULL,
    tax_amount NUMERIC(18, 4) NOT NULL,
    withholding_tax_amount NUMERIC(18, 4) DEFAULT 0.0000,
    total_amount NUMERIC(18, 4) NOT NULL,
    payable_amount NUMERIC(18, 4),
    match_type VARCHAR(50) NOT NULL,
    match_status VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    discrepancy_reason TEXT,
    rejection_reason TEXT,
    manager_override_note TEXT,
    manager_override_by_user_id UUID REFERENCES users(id),
    raw_ubl_xml TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_supplier_invoice_ettn UNIQUE (tenant_id, ettn),
    CONSTRAINT uk_supplier_invoice_vendor_no UNIQUE (tenant_id, vendor_id, invoice_number)
);

CREATE TABLE IF NOT EXISTS supplier_invoice_line_items (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    supplier_invoice_id UUID NOT NULL REFERENCES supplier_invoices(id) ON DELETE CASCADE,
    purchase_order_line_item_id UUID REFERENCES purchase_order_line_items(id),
    goods_receipt_line_item_id UUID REFERENCES goods_receipt_line_items(id),
    invoiced_quantity NUMERIC(18, 4) NOT NULL,
    unit_price NUMERIC(18, 4) NOT NULL,
    tax_rate NUMERIC(5, 2) NOT NULL,
    tax_amount NUMERIC(18, 4) NOT NULL,
    tevkifat_code VARCHAR(20),
    tevkifat_rate VARCHAR(20),
    tevkifat_amount NUMERIC(18, 4) DEFAULT 0.0000,
    line_total NUMERIC(18, 4) NOT NULL,
    match_status VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS invoice_discrepancies (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    supplier_invoice_id UUID NOT NULL REFERENCES supplier_invoices(id) ON DELETE CASCADE,
    discrepancy_type VARCHAR(50) NOT NULL,
    expected_value VARCHAR(100),
    actual_value VARCHAR(100),
    variance_amount NUMERIC(18, 4),
    variance_percentage NUMERIC(6, 2),
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolution_notes TEXT,
    resolved_by_user_id UUID REFERENCES users(id),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
