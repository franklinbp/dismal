ALTER TABLE sales_targets
    ADD COLUMN IF NOT EXISTS country_code VARCHAR(8);

ALTER TABLE sales_targets
    ADD COLUMN IF NOT EXISTS customer_type VARCHAR(32);

UPDATE sales_targets
SET country_code = COALESCE(country_code, 'EC'),
    customer_type = COALESCE(customer_type, 'FINAL')
WHERE country_code IS NULL
   OR customer_type IS NULL;
