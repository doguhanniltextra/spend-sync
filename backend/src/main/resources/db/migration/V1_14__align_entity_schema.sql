-- =========================================================================
-- SpendSync Migration: V1_14__align_entity_schema.sql
-- Align Flyway schema with current JPA entity mappings (ddl-auto=validate)
-- =========================================================================

-- ── users ──────────────────────────────────────────────────────────────────
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS manager_user_id UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS delegated_approver_id UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS delegation_start_date TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS delegation_end_date TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(30),
    ADD COLUMN IF NOT EXISTS last_login_ip VARCHAR(50),
    ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP WITHOUT TIME ZONE;

-- ── refresh_tokens ─────────────────────────────────────────────────────────
ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS token_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE;

UPDATE refresh_tokens
SET token_hash = COALESCE(token_hash, left(token, 128)),
    updated_at = COALESCE(updated_at, created_at)
WHERE token_hash IS NULL OR updated_at IS NULL;

ALTER TABLE refresh_tokens
    ALTER COLUMN token_hash SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);

-- ── purchase_requisitions ──────────────────────────────────────────────────
ALTER TABLE purchase_requisitions
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

-- ── requisition_line_items ─────────────────────────────────────────────────
ALTER TABLE requisition_line_items
    ADD COLUMN IF NOT EXISTS requisition_id UUID,
    ADD COLUMN IF NOT EXISTS item_category VARCHAR(50),
    ADD COLUMN IF NOT EXISTS unit_price NUMERIC(18, 4),
    ADD COLUMN IF NOT EXISTS total_price NUMERIC(18, 4),
    ADD COLUMN IF NOT EXISTS estimated_delivery_date DATE,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE;

UPDATE requisition_line_items
SET requisition_id = COALESCE(requisition_id, purchase_requisition_id),
    item_category = COALESCE(item_category, LEFT(category_code, 50)),
    unit_price = COALESCE(unit_price, estimated_unit_price),
    total_price = COALESCE(total_price, estimated_total_amount),
    created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW());

ALTER TABLE requisition_line_items
    ALTER COLUMN requisition_id SET NOT NULL,
    ALTER COLUMN item_category SET NOT NULL,
    ALTER COLUMN unit_price SET NOT NULL,
    ALTER COLUMN total_price SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'requisition_line_items_requisition_id_fkey'
    ) THEN
        ALTER TABLE requisition_line_items
            ADD CONSTRAINT requisition_line_items_requisition_id_fkey
            FOREIGN KEY (requisition_id) REFERENCES purchase_requisitions(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_pr_items_requisition ON requisition_line_items(requisition_id);
CREATE INDEX IF NOT EXISTS idx_pr_items_tenant ON requisition_line_items(tenant_id);

-- ── requisition_approval_steps ─────────────────────────────────────────────
ALTER TABLE requisition_approval_steps
    ADD COLUMN IF NOT EXISTS requisition_id UUID,
    ADD COLUMN IF NOT EXISTS approver_id UUID,
    ADD COLUMN IF NOT EXISTS approval_level INT,
    ADD COLUMN IF NOT EXISTS decision_note TEXT,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE;

UPDATE requisition_approval_steps
SET requisition_id = COALESCE(requisition_id, purchase_requisition_id),
    approver_id = COALESCE(approver_id, approver_user_id),
    approval_level = COALESCE(approval_level, step_order),
    decision_note = COALESCE(decision_note, decision_notes),
    updated_at = COALESCE(updated_at, created_at);

ALTER TABLE requisition_approval_steps
    ALTER COLUMN requisition_id SET NOT NULL,
    ALTER COLUMN approver_id SET NOT NULL,
    ALTER COLUMN approval_level SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'requisition_approval_steps_requisition_id_fkey'
    ) THEN
        ALTER TABLE requisition_approval_steps
            ADD CONSTRAINT requisition_approval_steps_requisition_id_fkey
            FOREIGN KEY (requisition_id) REFERENCES purchase_requisitions(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'requisition_approval_steps_approver_id_fkey'
    ) THEN
        ALTER TABLE requisition_approval_steps
            ADD CONSTRAINT requisition_approval_steps_approver_id_fkey
            FOREIGN KEY (approver_id) REFERENCES users(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_pr_steps_requisition ON requisition_approval_steps(requisition_id);
CREATE INDEX IF NOT EXISTS idx_pr_steps_approver ON requisition_approval_steps(approver_id, status);
CREATE INDEX IF NOT EXISTS idx_pr_steps_tenant ON requisition_approval_steps(tenant_id);

-- ── purchase_orders ────────────────────────────────────────────────────────
ALTER TABLE purchase_orders
    ADD COLUMN IF NOT EXISTS revision_number INT NOT NULL DEFAULT 0;

-- ── purchase_order_line_items ──────────────────────────────────────────────
ALTER TABLE purchase_order_line_items
    ADD COLUMN IF NOT EXISTS total_price NUMERIC(18, 4),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE;

UPDATE purchase_order_line_items
SET total_price = COALESCE(total_price, line_total_amount),
    created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW());

ALTER TABLE purchase_order_line_items
    ALTER COLUMN total_price SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

-- ── purchase_order_revisions ───────────────────────────────────────────────
ALTER TABLE purchase_order_revisions
    ADD COLUMN IF NOT EXISTS previous_total_amount NUMERIC(18, 4),
    ADD COLUMN IF NOT EXISTS new_total_amount NUMERIC(18, 4),
    ADD COLUMN IF NOT EXISTS differential_amount NUMERIC(18, 4),
    ADD COLUMN IF NOT EXISTS reason TEXT,
    ADD COLUMN IF NOT EXISTS revised_by_user_id UUID,
    ADD COLUMN IF NOT EXISTS snapshot_payload TEXT;

UPDATE purchase_order_revisions
SET previous_total_amount = COALESCE(previous_total_amount, 0.0000),
    new_total_amount = COALESCE(new_total_amount, 0.0000),
    differential_amount = COALESCE(differential_amount, 0.0000),
    reason = COALESCE(reason, change_summary, 'Legacy revision'),
    revised_by_user_id = COALESCE(revised_by_user_id, created_by_user_id),
    snapshot_payload = COALESCE(snapshot_payload, previous_state_json);

ALTER TABLE purchase_order_revisions
    ALTER COLUMN previous_total_amount SET NOT NULL,
    ALTER COLUMN new_total_amount SET NOT NULL,
    ALTER COLUMN differential_amount SET NOT NULL,
    ALTER COLUMN reason SET NOT NULL,
    ALTER COLUMN revised_by_user_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'purchase_order_revisions_revised_by_user_id_fkey'
    ) THEN
        ALTER TABLE purchase_order_revisions
            ADD CONSTRAINT purchase_order_revisions_revised_by_user_id_fkey
            FOREIGN KEY (revised_by_user_id) REFERENCES users(id);
    END IF;
END $$;

-- ── goods_receipt_line_items ───────────────────────────────────────────────
ALTER TABLE goods_receipt_line_items
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITHOUT TIME ZONE;

UPDATE goods_receipt_line_items
SET created_at = COALESCE(created_at, NOW())
WHERE created_at IS NULL;

ALTER TABLE goods_receipt_line_items
    ALTER COLUMN created_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_gr_line_items_gr ON goods_receipt_line_items(goods_receipt_id);
CREATE INDEX IF NOT EXISTS idx_gr_line_items_po_line ON goods_receipt_line_items(purchase_order_line_item_id);

-- ── supplier_invoice_line_items ────────────────────────────────────────────
ALTER TABLE supplier_invoice_line_items
    ADD COLUMN IF NOT EXISTS variance_reason TEXT,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITHOUT TIME ZONE;

UPDATE supplier_invoice_line_items
SET created_at = COALESCE(created_at, NOW())
WHERE created_at IS NULL;

ALTER TABLE supplier_invoice_line_items
    ALTER COLUMN created_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_inv_line_items_inv ON supplier_invoice_line_items(supplier_invoice_id);
CREATE INDEX IF NOT EXISTS idx_inv_line_items_po_line ON supplier_invoice_line_items(purchase_order_line_item_id);

-- ── vendor_early_pay_offers ────────────────────────────────────────────────
ALTER TABLE vendor_early_pay_offers
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITHOUT TIME ZONE;

UPDATE vendor_early_pay_offers
SET created_at = COALESCE(created_at, COALESCE(accepted_at, NOW()))
WHERE created_at IS NULL;

ALTER TABLE vendor_early_pay_offers
    ALTER COLUMN created_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_early_pay_invoice ON vendor_early_pay_offers(supplier_invoice_id);
CREATE INDEX IF NOT EXISTS idx_early_pay_vendor ON vendor_early_pay_offers(tenant_id, vendor_id);

-- ── vendor_invitations ─────────────────────────────────────────────────────
ALTER TABLE vendor_invitations
    ADD COLUMN IF NOT EXISTS invitation_token VARCHAR(255),
    ADD COLUMN IF NOT EXISTS tax_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS company_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS created_by_user_id UUID,
    ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMP WITHOUT TIME ZONE;

UPDATE vendor_invitations
SET invitation_token = COALESCE(invitation_token, token),
    tax_number = COALESCE(tax_number, 'UNKNOWN'),
    company_name = COALESCE(company_name, email),
    created_by_user_id = COALESCE(
        created_by_user_id,
        (SELECT id FROM users ORDER BY created_at ASC LIMIT 1)
    );

ALTER TABLE vendor_invitations
    ALTER COLUMN invitation_token SET NOT NULL,
    ALTER COLUMN tax_number SET NOT NULL,
    ALTER COLUMN company_name SET NOT NULL,
    ALTER COLUMN created_by_user_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_vendor_invitations_invitation_token ON vendor_invitations(invitation_token);
