ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS sale_number BIGINT;

UPDATE sales
SET country = 'EC',
    currency = COALESCE(currency, 'USD')
WHERE country IS NULL;

WITH current_max AS (
    SELECT GREATEST(COALESCE(MAX(sale_number), 9999), 9999) AS value
    FROM sales
    WHERE country = 'EC'
), numbered AS (
    SELECT
        sale.id,
        current_max.value + ROW_NUMBER() OVER (ORDER BY sale.created_at, sale.id) AS value
    FROM sales sale
    CROSS JOIN current_max
    WHERE sale.country = 'EC'
      AND sale.sale_number IS NULL
)
UPDATE sales sale
SET sale_number = numbered.value
FROM numbered
WHERE sale.id = numbered.id;

WITH current_max AS (
    SELECT GREATEST(COALESCE(MAX(sale_number), 19999), 19999) AS value
    FROM sales
    WHERE country = 'PE'
), numbered AS (
    SELECT
        sale.id,
        current_max.value + ROW_NUMBER() OVER (ORDER BY sale.created_at, sale.id) AS value
    FROM sales sale
    CROSS JOIN current_max
    WHERE sale.country = 'PE'
      AND sale.sale_number IS NULL
)
UPDATE sales sale
SET sale_number = numbered.value
FROM numbered
WHERE sale.id = numbered.id;

CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_country_sale_number
    ON sales(country, sale_number)
    WHERE sale_number IS NOT NULL;

CREATE TABLE IF NOT EXISTS sale_number_counters (
    country VARCHAR(2) PRIMARY KEY,
    next_number BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_sale_number_counters_country CHECK (country IN ('EC', 'PE')),
    CONSTRAINT ck_sale_number_counters_next CHECK (next_number > 0)
);

INSERT INTO sale_number_counters (country, next_number, version)
SELECT
    'EC',
    GREATEST(COALESCE(MAX(sale_number) + 1, 10000), 10000),
    0
FROM sales
WHERE country = 'EC'
ON CONFLICT (country) DO UPDATE
SET next_number = GREATEST(sale_number_counters.next_number, EXCLUDED.next_number);

INSERT INTO sale_number_counters (country, next_number, version)
SELECT
    'PE',
    GREATEST(COALESCE(MAX(sale_number) + 1, 20000), 20000),
    0
FROM sales
WHERE country = 'PE'
ON CONFLICT (country) DO UPDATE
SET next_number = GREATEST(sale_number_counters.next_number, EXCLUDED.next_number);
