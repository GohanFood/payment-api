ALTER TABLE payments
    ADD COLUMN customer_id VARCHAR(100),
    ADD COLUMN card_id VARCHAR(100);

CREATE INDEX idx_payments_customer_id ON payments(customer_id);
