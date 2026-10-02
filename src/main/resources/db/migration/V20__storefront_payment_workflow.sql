ALTER TABLE storefront_orders
    ADD COLUMN IF NOT EXISTS checkout_method VARCHAR(32),
    ADD COLUMN IF NOT EXISTS payment_account_id UUID,
    ADD COLUMN IF NOT EXISTS payment_claim_bank VARCHAR(120),
    ADD COLUMN IF NOT EXISTS payment_claim_reference VARCHAR(160),
    ADD COLUMN IF NOT EXISTS payment_claim_payer VARCHAR(160),
    ADD COLUMN IF NOT EXISTS payment_claim_amount NUMERIC(19, 4),
    ADD COLUMN IF NOT EXISTS payment_claimed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS payment_review_notes VARCHAR(500),
    ADD COLUMN IF NOT EXISTS payment_reviewed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS payment_reviewed_by VARCHAR(180);

UPDATE storefront_orders
SET checkout_method = 'BANK_TRANSFER'
WHERE checkout_method IS NULL;

ALTER TABLE storefront_orders
    ALTER COLUMN checkout_method SET NOT NULL;

ALTER TABLE payment_accounts
    ADD COLUMN IF NOT EXISTS country_code VARCHAR(2),
    ADD COLUMN IF NOT EXISTS public_for_storefront BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS bank_name VARCHAR(120),
    ADD COLUMN IF NOT EXISTS account_holder VARCHAR(160),
    ADD COLUMN IF NOT EXISTS account_number VARCHAR(80),
    ADD COLUMN IF NOT EXISTS account_type VARCHAR(80),
    ADD COLUMN IF NOT EXISTS tax_id VARCHAR(40);

CREATE INDEX IF NOT EXISTS idx_storefront_orders_payment_review
    ON storefront_orders (status, payment_claimed_at);

CREATE UNIQUE INDEX IF NOT EXISTS uk_storefront_payment_claim_reference
    ON storefront_orders (payment_account_id, payment_claim_reference)
    WHERE payment_account_id IS NOT NULL AND payment_claim_reference IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payment_accounts_storefront
    ON payment_accounts (public_for_storefront, active, country_code, currency);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_storefront_orders_payment_account'
    ) THEN
        ALTER TABLE storefront_orders
            ADD CONSTRAINT fk_storefront_orders_payment_account
            FOREIGN KEY (payment_account_id)
            REFERENCES payment_accounts(id)
            ON DELETE SET NULL;
    END IF;
END $$;
