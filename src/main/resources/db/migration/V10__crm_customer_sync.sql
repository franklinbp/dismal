CREATE TABLE IF NOT EXISTS crm_customer_sync (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    crm_campaign_client_id INTEGER,
    sync_status VARCHAR(32) NOT NULL,
    payload_hash VARCHAR(64),
    last_synced_at TIMESTAMP,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_crm_customer_sync_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_crm_customer_sync_status
    ON crm_customer_sync(sync_status);
