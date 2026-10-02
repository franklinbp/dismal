ALTER TABLE users
    ADD COLUMN IF NOT EXISTS customer_type VARCHAR(32);

UPDATE users
SET customer_type = 'FINAL'
WHERE customer_type IS NULL;
