-- =========================================================================
-- SpendSync Migration: V1_1__vendors.sql
-- Vendor Master Data & Tax Office Profiles
-- =========================================================================

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
