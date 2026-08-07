ALTER TABLE payments
    ADD COLUMN mp_payment_id BIGINT,
    ADD COLUMN qr_code TEXT,
    ADD COLUMN qr_code_base64 TEXT,
    ADD COLUMN ticket_url VARCHAR(500),
    ADD COLUMN payer_email VARCHAR(255),
    ADD COLUMN payer_document_type VARCHAR(10),
    ADD COLUMN payer_document_number VARCHAR(20),
    ADD COLUMN expires_at TIMESTAMP;
