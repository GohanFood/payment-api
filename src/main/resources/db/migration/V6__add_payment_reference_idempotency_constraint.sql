-- A business reference identifies exactly one payment per user.  This is the
-- database-level guard against concurrent retries and multiple scheduler replicas.
ALTER TABLE payments
    ADD CONSTRAINT uk_payments_user_reference UNIQUE (user_id, reference_id);
