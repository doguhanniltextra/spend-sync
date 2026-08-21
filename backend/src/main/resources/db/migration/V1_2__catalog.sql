-- =========================================================================
-- SpendSync Migration: V1_2__catalog.sql
-- Item Master & Hierarchical Category Taxonomy
-- =========================================================================

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
