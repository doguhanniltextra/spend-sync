-- =========================================================================
-- SpendSync Migration: V1_10__vendor_portal.sql
-- External Vendor Portal: Users, PO Ack, ASN Dispatch, Early Pay, BA-BS
-- =========================================================================

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
