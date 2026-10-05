-- =========================================================================
-- SpendSync Migration: V1_13__align_budget_transactions.sql
-- Align budget_transactions with BudgetTransaction entity (BaseEntity + ledger)
-- =========================================================================

ALTER TABLE budget_transactions
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS balance_before NUMERIC(18, 4),
    ADD COLUMN IF NOT EXISTS balance_after NUMERIC(18, 4),
    ADD COLUMN IF NOT EXISTS notes TEXT;

UPDATE budget_transactions
SET updated_at = COALESCE(updated_at, created_at),
    balance_before = COALESCE(balance_before, 0.0000),
    balance_after = COALESCE(balance_after, amount),
    notes = COALESCE(notes, description)
WHERE updated_at IS NULL
   OR balance_before IS NULL
   OR balance_after IS NULL
   OR (notes IS NULL AND description IS NOT NULL);

ALTER TABLE budget_transactions
    ALTER COLUMN updated_at SET NOT NULL,
    ALTER COLUMN balance_before SET NOT NULL,
    ALTER COLUMN balance_after SET NOT NULL;

ALTER TABLE budget_transactions
    ALTER COLUMN reference_id DROP NOT NULL,
    ALTER COLUMN reference_type DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_budget_tx_pool_id ON budget_transactions(budget_pool_id);
CREATE INDEX IF NOT EXISTS idx_budget_tx_tenant_id ON budget_transactions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_budget_tx_reference ON budget_transactions(reference_id, reference_type);
