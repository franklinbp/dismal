CREATE TABLE IF NOT EXISTS woo_order_sync (
    id UUID PRIMARY KEY,
    external_order_id VARCHAR(120) NOT NULL,
    sale_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_woo_order_sync_external_order_id UNIQUE (external_order_id),
    CONSTRAINT fk_woo_order_sync_sale
        FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE
);
