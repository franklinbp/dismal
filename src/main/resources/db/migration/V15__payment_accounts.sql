CREATE TABLE IF NOT EXISTS payment_accounts (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(32) NOT NULL,
    currency VARCHAR(8) NOT NULL DEFAULT 'USD',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    default_account BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO payment_accounts (
    id,
    name,
    type,
    currency,
    active,
    default_account
)
SELECT
    '11111111-1111-1111-1111-111111111111',
    'Caja principal',
    'CASH',
    'USD',
    TRUE,
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM payment_accounts WHERE default_account = TRUE
);

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS payment_account_id UUID;

UPDATE payments
SET payment_account_id = '11111111-1111-1111-1111-111111111111'
WHERE payment_account_id IS NULL
  AND EXISTS (
      SELECT 1 FROM payment_accounts WHERE id = '11111111-1111-1111-1111-111111111111'
  );

ALTER TABLE payments
    ADD CONSTRAINT fk_payments_payment_account
    FOREIGN KEY (payment_account_id)
    REFERENCES payment_accounts(id)
    ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_payments_payment_account
    ON payments(payment_account_id);
