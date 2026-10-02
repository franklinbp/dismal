CREATE TABLE IF NOT EXISTS storefront_orders (
    id UUID PRIMARY KEY,
    order_number VARCHAR(40) NOT NULL,
    country VARCHAR(16) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    customer_type VARCHAR(24) NOT NULL,
    status VARCHAR(32) NOT NULL,
    customer_id UUID NOT NULL,
    email VARCHAR(180) NOT NULL,
    first_name VARCHAR(80),
    last_name VARCHAR(80),
    phone VARCHAR(32),
    tax_id VARCHAR(40),
    subtotal NUMERIC(19, 4) NOT NULL,
    discount_total NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total NUMERIC(19, 4) NOT NULL,
    payment_provider VARCHAR(40),
    payment_reference VARCHAR(160),
    sale_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_storefront_orders_order_number UNIQUE (order_number),
    CONSTRAINT uk_storefront_orders_payment_reference UNIQUE (payment_provider, payment_reference),
    CONSTRAINT fk_storefront_orders_customer FOREIGN KEY (customer_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS storefront_order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    software_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    platform VARCHAR(120),
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(19, 4) NOT NULL,
    subtotal NUMERIC(19, 4) NOT NULL,
    CONSTRAINT fk_storefront_order_items_order FOREIGN KEY (order_id) REFERENCES storefront_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_storefront_order_items_software FOREIGN KEY (software_id) REFERENCES software (id)
);

CREATE INDEX IF NOT EXISTS idx_storefront_orders_status
    ON storefront_orders (status);

CREATE INDEX IF NOT EXISTS idx_storefront_orders_customer
    ON storefront_orders (customer_id);

CREATE INDEX IF NOT EXISTS idx_storefront_orders_country_status
    ON storefront_orders (country, status);

CREATE INDEX IF NOT EXISTS idx_storefront_order_items_order
    ON storefront_order_items (order_id);
