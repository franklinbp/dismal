CREATE TABLE IF NOT EXISTS strategy_actions (
    id UUID PRIMARY KEY,
    product_id UUID,
    target_id UUID,
    source VARCHAR(32) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    recommended_channel VARCHAR(32),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    assigned_to UUID,
    due_date DATE,
    completed_at TIMESTAMP,
    result_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_strategy_actions_product
        FOREIGN KEY (product_id) REFERENCES software(id) ON DELETE SET NULL,
    CONSTRAINT fk_strategy_actions_target
        FOREIGN KEY (target_id) REFERENCES sales_targets(id) ON DELETE SET NULL,
    CONSTRAINT fk_strategy_actions_assigned_to
        FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_strategy_actions_status
    ON strategy_actions(status);

CREATE INDEX IF NOT EXISTS idx_strategy_actions_product
    ON strategy_actions(product_id);

CREATE INDEX IF NOT EXISTS idx_strategy_actions_due_date
    ON strategy_actions(due_date);
