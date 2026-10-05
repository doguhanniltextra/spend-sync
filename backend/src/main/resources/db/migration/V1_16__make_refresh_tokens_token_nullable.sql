-- =========================================================================
-- SpendSync Migration: V1_16__make_refresh_tokens_token_nullable.sql
-- Allow token column to be nullable as RefreshToken entity maps token_hash
-- =========================================================================

ALTER TABLE refresh_tokens ALTER COLUMN token DROP NOT NULL;
