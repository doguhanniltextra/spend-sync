-- =========================================================================
-- SpendSync Database Migration: V1__init_schema.sql
-- Baseline DDL for all enterprise domain entities
-- =========================================================================

-- 1. Core & Organization
CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    subscription_tier VARCHAR(50) NOT NULL DEFAULT 'ENTERPRISE',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS legal_entities (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(255) NOT NULL,
    company_code VARCHAR(50) NOT NULL,
    tax_number VARCHAR(50) NOT NULL,
    tax_office VARCHAR(100),
    base_currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
    registered_address TEXT,
    country VARCHAR(2),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uq_legal_entity_tenant_code UNIQUE (tenant_id, company_code)
);

CREATE TABLE IF NOT EXISTS cost_centers (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    manager_user_id UUID,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uq_cost_center_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE IF NOT EXISTS facilities (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    name VARCHAR(255) NOT NULL,
    facility_code VARCHAR(50) NOT NULL,
    facility_type VARCHAR(50) NOT NULL,
    shipping_address TEXT NOT NULL,
    contact_person VARCHAR(255),
    contact_phone VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uq_facility_tenant_code UNIQUE (tenant_id, facility_code)
);

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    job_title VARCHAR(150),
    employee_id VARCHAR(100),
    country VARCHAR(2),
    timezone VARCHAR(50),
    preferred_language VARCHAR(10),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    last_login_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uq_user_tenant_email UNIQUE (tenant_id, email)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE TABLE IF NOT EXISTS user_assigned_legal_entities (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, legal_entity_id)
);

CREATE TABLE IF NOT EXISTS user_invitations (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    email VARCHAR(255),
    target_legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    invite_token VARCHAR(255) NOT NULL UNIQUE,
    is_multi_use BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    is_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS user_invitation_roles (
    invitation_id UUID NOT NULL REFERENCES user_invitations(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (invitation_id, role)
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

-- 2. Vendors
CREATE TABLE IF NOT EXISTS vendors (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(255) NOT NULL,
    tax_number VARCHAR(50) NOT NULL,
    tax_office VARCHAR(100),
    category VARCHAR(50) NOT NULL,
    tier VARCHAR(50) NOT NULL,
    is_einvoice_registered BOOLEAN NOT NULL DEFAULT TRUE,
    order_email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50),
    address TEXT,
    city VARCHAR(100),
    country VARCHAR(10) NOT NULL DEFAULT 'TR',
    payment_terms VARCHAR(50) NOT NULL,
    bank_name VARCHAR(100),
    iban VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uq_vendor_tenant_tax_number UNIQUE (tenant_id, tax_number)
);

-- 3. Catalog
CREATE TABLE IF NOT EXISTS catalog_categories (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    parent_id UUID REFERENCES catalog_categories(id),
    code VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    icon_code VARCHAR(50),
    full_path VARCHAR(500) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_cat_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT uk_cat_tenant_fullpath UNIQUE (tenant_id, full_path)
);

CREATE TABLE IF NOT EXISTS catalog_items (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    item_code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category_id UUID REFERENCES catalog_categories(id),
    preferred_vendor_id UUID REFERENCES vendors(id),
    unit_price NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
    vat_rate NUMERIC(5, 4) NOT NULL DEFAULT 0.2000,
    unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'PIECE',
    contract_reference VARCHAR(100),
    valid_from DATE,
    valid_until DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_preferred BOOLEAN NOT NULL DEFAULT FALSE,
    gl_account_code VARCHAR(50),
    created_by_user_id UUID REFERENCES users(id),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uq_catalog_item_tenant_code UNIQUE (tenant_id, item_code)
);

-- 4. Budget
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

-- 5. Authority Limits
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

-- 6. Purchase Requisitions
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

-- 7. Purchase Orders
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

-- 8. Receiving
CREATE TABLE IF NOT EXISTS goods_receipts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    receipt_number VARCHAR(50) NOT NULL,
    purchase_order_id UUID NOT NULL REFERENCES purchase_orders(id),
    delivery_facility_id UUID NOT NULL REFERENCES facilities(id),
    waybill_number VARCHAR(100) NOT NULL,
    waybill_date DATE NOT NULL,
    received_by_user_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    notes TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_goods_receipt_number UNIQUE (tenant_id, receipt_number)
);

CREATE TABLE IF NOT EXISTS goods_receipt_line_items (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    goods_receipt_id UUID NOT NULL REFERENCES goods_receipts(id) ON DELETE CASCADE,
    purchase_order_line_item_id UUID NOT NULL REFERENCES purchase_order_line_items(id),
    received_quantity NUMERIC(18, 4) NOT NULL,
    accepted_quantity NUMERIC(18, 4) NOT NULL,
    rejected_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    rejection_reason TEXT,
    notes TEXT
);

-- 9. Invoicing & 3-Way Match
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

-- 10. Payments
CREATE TABLE IF NOT EXISTS payment_batches (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    batch_number VARCHAR(50) NOT NULL,
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    payment_method VARCHAR(50) NOT NULL DEFAULT 'ISO_20022_PAIN_001',
    total_amount NUMERIC(18, 4) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'TRY',
    item_count INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    approved_by_user_id UUID REFERENCES users(id),
    approved_at TIMESTAMP WITHOUT TIME ZONE,
    xml_payload TEXT,
    idempotency_key VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_payment_batch_number UNIQUE (tenant_id, batch_number),
    CONSTRAINT uk_payment_batch_idempotency UNIQUE (tenant_id, idempotency_key)
);

CREATE TABLE IF NOT EXISTS payment_batch_items (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    payment_batch_id UUID NOT NULL REFERENCES payment_batches(id) ON DELETE CASCADE,
    supplier_invoice_id UUID NOT NULL REFERENCES supplier_invoices(id),
    vendor_id UUID NOT NULL REFERENCES vendors(id),
    vendor_name VARCHAR(255) NOT NULL,
    vendor_iban TEXT,
    amount NUMERIC(18, 4) NOT NULL,
    discount_amount NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    net_payable_amount NUMERIC(18, 4) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'INCLUDED',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

-- 11. Vendor Portal
CREATE TABLE IF NOT EXISTS vendor_users (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    vendor_id UUID NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone_number VARCHAR(50),
    role VARCHAR(50) NOT NULL DEFAULT 'VENDOR_ADMIN',
    is_primary_contact BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_vendor_user_tenant_email UNIQUE (tenant_id, email)
);

CREATE TABLE IF NOT EXISTS vendor_invitations (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    vendor_id UUID NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS vendor_po_acknowledgments (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    purchase_order_id UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    vendor_id UUID NOT NULL REFERENCES vendors(id),
    acknowledged_by_user_id UUID NOT NULL REFERENCES vendor_users(id),
    status VARCHAR(50) NOT NULL,
    promised_delivery_date DATE,
    vendor_notes TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS vendor_asn_shipments (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    purchase_order_id UUID NOT NULL REFERENCES purchase_orders(id),
    vendor_id UUID NOT NULL REFERENCES vendors(id),
    dispatched_by_user_id UUID NOT NULL REFERENCES vendor_users(id),
    waybill_number VARCHAR(100) NOT NULL,
    ettn VARCHAR(100),
    carrier_name VARCHAR(100),
    tracking_number VARCHAR(100),
    vehicle_plate VARCHAR(50),
    driver_national_id VARCHAR(255),
    driver_name VARCHAR(150),
    driver_phone VARCHAR(50),
    shipment_date DATE NOT NULL,
    estimated_arrival_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DISPATCHED',
    notes TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS vendor_asn_shipment_line_items (
    id UUID PRIMARY KEY,
    asn_shipment_id UUID NOT NULL REFERENCES vendor_asn_shipments(id) ON DELETE CASCADE,
    purchase_order_line_item_id UUID NOT NULL REFERENCES purchase_order_line_items(id),
    shipped_quantity NUMERIC(15, 4) NOT NULL,
    unit_of_measure VARCHAR(50) NOT NULL DEFAULT 'PIECE',
    lot_number VARCHAR(100),
    serial_numbers TEXT
);

CREATE TABLE IF NOT EXISTS vendor_early_pay_offers (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    supplier_invoice_id UUID NOT NULL REFERENCES supplier_invoices(id) ON DELETE CASCADE,
    vendor_id UUID NOT NULL REFERENCES vendors(id),
    original_amount NUMERIC(18, 4) NOT NULL,
    original_due_date DATE NOT NULL,
    discount_percentage NUMERIC(5, 2) NOT NULL,
    discount_amount NUMERIC(18, 4) NOT NULL,
    net_payout_amount NUMERIC(18, 4) NOT NULL,
    accelerated_payment_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    accepted_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE IF NOT EXISTS vendor_monthly_reconciliations (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    vendor_id UUID NOT NULL REFERENCES vendors(id),
    period_year INT NOT NULL,
    period_month INT NOT NULL,
    invoice_count INT NOT NULL DEFAULT 0,
    total_amount NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    vendor_approved_at TIMESTAMP WITHOUT TIME ZONE,
    vendor_notes TEXT,
    signed_checksum VARCHAR(64),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_reconciliation_vendor_period UNIQUE (tenant_id, vendor_id, period_year, period_month)
);

CREATE TABLE IF NOT EXISTS vendor_catalog_proposals (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    vendor_id UUID NOT NULL REFERENCES vendors(id),
    catalog_item_id UUID REFERENCES catalog_items(id),
    proposed_item_code VARCHAR(100) NOT NULL,
    proposed_name VARCHAR(255) NOT NULL,
    proposed_category VARCHAR(100) NOT NULL,
    proposed_unit_price NUMERIC(18, 4) NOT NULL,
    proposed_currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
    vat_rate NUMERIC(5, 2) NOT NULL DEFAULT 20.00,
    lead_time_days INT NOT NULL DEFAULT 3,
    notes TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_BUYER_REVIEW',
    buyer_decision_notes TEXT,
    reviewed_by_user_id UUID REFERENCES users(id),
    reviewed_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS vendor_bank_change_requests (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    vendor_id UUID NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
    requested_by_user_id UUID NOT NULL REFERENCES vendor_users(id),
    proposed_bank_name VARCHAR(100) NOT NULL,
    proposed_iban VARCHAR(255) NOT NULL,
    supporting_document_url VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_REVIEW',
    reviewed_by_user_id UUID,
    review_notes TEXT,
    reviewed_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

-- 12. Audit Trail
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

-- 13. High-Performance Multi-Tenant Indexes
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
