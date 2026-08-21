-- =========================================================================
-- SpendSync Migration: V1_7__receiving.sql
-- Goods Receipt Notes (GRN) & Warehouse Physical Inspection Lines
-- =========================================================================

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
