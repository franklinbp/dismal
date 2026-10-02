CREATE TABLE IF NOT EXISTS customer_markets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    country VARCHAR(2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    customer_type VARCHAR(24) NOT NULL DEFAULT 'FINAL',
    tax_id VARCHAR(40),
    billing_email VARCHAR(180),
    has_credit BOOLEAN NOT NULL DEFAULT FALSE,
    credit_limit NUMERIC(19, 4) NOT NULL DEFAULT 0,
    credit_used NUMERIC(19, 4) NOT NULL DEFAULT 0,
    credit_days INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    primary_market BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_customer_markets_user_country UNIQUE (user_id, country),
    CONSTRAINT ck_customer_markets_country CHECK (country IN ('EC', 'PE')),
    CONSTRAINT ck_customer_markets_currency CHECK (currency IN ('USD', 'PEN')),
    CONSTRAINT ck_customer_markets_credit CHECK (
        credit_limit >= 0 AND credit_used >= 0 AND credit_days >= 0
    )
);

WITH inferred_market AS (
    SELECT
        u.id AS user_id,
        COALESCE(
            (SELECT so.country
             FROM storefront_orders so
             WHERE so.customer_id = u.id
             ORDER BY so.created_at DESC
             LIMIT 1),
            (SELECT wa.country
             FROM wholesale_applications wa
             WHERE wa.user_id = u.id
             ORDER BY wa.created_at DESC
             LIMIT 1),
            CASE
                WHEN regexp_replace(COALESCE(u.phone, ''), '[^0-9]', '', 'g') LIKE '51%' THEN 'PE'
                ELSE 'EC'
            END
        ) AS country
    FROM users u
    WHERE u.role IN ('USER', 'CUSTOMER')
)
INSERT INTO customer_markets (
    id, user_id, country, currency, customer_type, tax_id, billing_email,
    has_credit, credit_limit, credit_used, credit_days, active, primary_market,
    version, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    inferred.country,
    CASE WHEN inferred.country = 'PE' THEN 'PEN' ELSE 'USD' END,
    COALESCE(u.customer_type, 'FINAL'),
    u.tax_id,
    COALESCE(NULLIF(u.billing_email, ''), u.email),
    COALESCE(u.has_credit, FALSE),
    COALESCE(u.credit_limit, 0),
    COALESCE(u.credit_used, 0),
    COALESCE(u.credit_days, 0),
    COALESCE(u.enabled, TRUE),
    TRUE,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
JOIN inferred_market inferred ON inferred.user_id = u.id
ON CONFLICT (user_id, country) DO NOTHING;

CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_markets_primary_user
    ON customer_markets(user_id)
    WHERE primary_market = TRUE;

CREATE INDEX IF NOT EXISTS idx_customer_markets_country_type
    ON customer_markets(country, customer_type, active);

ALTER TABLE storefront_orders
    ADD COLUMN IF NOT EXISTS customer_market_id UUID;

UPDATE storefront_orders so
SET customer_market_id = market.id
FROM customer_markets market
WHERE so.customer_market_id IS NULL
  AND market.user_id = so.customer_id
  AND market.country = so.country;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_storefront_orders_customer_market'
    ) THEN
        ALTER TABLE storefront_orders
            ADD CONSTRAINT fk_storefront_orders_customer_market
            FOREIGN KEY (customer_market_id) REFERENCES customer_markets(id) ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_storefront_orders_customer_market
    ON storefront_orders(customer_market_id, created_at DESC);

ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS customer_market_id UUID,
    ADD COLUMN IF NOT EXISTS country VARCHAR(2),
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3);

UPDATE sales sale
SET customer_market_id = so.customer_market_id,
    country = so.country,
    currency = so.currency
FROM storefront_orders so
WHERE so.sale_id = sale.id
  AND sale.customer_market_id IS NULL;

UPDATE sales sale
SET customer_market_id = market.id,
    country = market.country,
    currency = market.currency
FROM customer_markets market
WHERE sale.customer_market_id IS NULL
  AND market.user_id = sale.client_id
  AND market.primary_market = TRUE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_sales_customer_market'
    ) THEN
        ALTER TABLE sales
            ADD CONSTRAINT fk_sales_customer_market
            FOREIGN KEY (customer_market_id) REFERENCES customer_markets(id) ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_sales_customer_market
    ON sales(customer_market_id, created_at DESC);

DROP INDEX IF EXISTS uk_wholesale_applications_pending_user;

CREATE UNIQUE INDEX IF NOT EXISTS uk_wholesale_applications_pending_market
    ON wholesale_applications(user_id, country)
    WHERE status = 'PENDING';
