ALTER TABLE software
    ADD COLUMN IF NOT EXISTS sku VARCHAR(80),
    ADD COLUMN IF NOT EXISTS barcode VARCHAR(80),
    ADD COLUMN IF NOT EXISTS brand VARCHAR(120),
    ADD COLUMN IF NOT EXISTS stock_quantity INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS reserved_quantity INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS weight_kg NUMERIC(10, 3),
    ADD COLUMN IF NOT EXISTS length_cm NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS width_cm NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS height_cm NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS physical_product BOOLEAN NOT NULL DEFAULT TRUE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_software_sku ON software (sku) WHERE sku IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_software_barcode ON software (barcode) WHERE barcode IS NOT NULL;

ALTER TABLE storefront_orders
    ADD COLUMN IF NOT EXISTS shipping_recipient VARCHAR(180),
    ADD COLUMN IF NOT EXISTS shipping_address_line1 VARCHAR(220),
    ADD COLUMN IF NOT EXISTS shipping_address_line2 VARCHAR(220),
    ADD COLUMN IF NOT EXISTS shipping_city VARCHAR(120),
    ADD COLUMN IF NOT EXISTS shipping_region VARCHAR(120),
    ADD COLUMN IF NOT EXISTS shipping_postal_code VARCHAR(32),
    ADD COLUMN IF NOT EXISTS shipping_country VARCHAR(2),
    ADD COLUMN IF NOT EXISTS shipping_notes VARCHAR(500),
    ADD COLUMN IF NOT EXISTS shipping_carrier VARCHAR(120),
    ADD COLUMN IF NOT EXISTS tracking_number VARCHAR(160),
    ADD COLUMN IF NOT EXISTS shipped_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMP;

ALTER TABLE software ADD CONSTRAINT chk_software_stock_non_negative
    CHECK (stock_quantity >= 0 AND reserved_quantity >= 0 AND reserved_quantity <= stock_quantity);
