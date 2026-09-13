ALTER TABLE payments RENAME COLUMN order_id TO reference_id;

ALTER TABLE payments
    ALTER COLUMN reference_id TYPE VARCHAR(255) USING reference_id::text;

ALTER INDEX IF EXISTS idx_payments_order_id RENAME TO idx_payments_reference_id;
