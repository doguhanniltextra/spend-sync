-- =========================================================================
-- SpendSync Migration: V1_9__payments.sql
-- Bulk Payment Batches & Bank Instruction Execution
-- =========================================================================

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
