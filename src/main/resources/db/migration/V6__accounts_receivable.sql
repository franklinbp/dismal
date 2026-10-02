CREATE TABLE IF NOT EXISTS accounts_receivable (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL UNIQUE,
    client_id UUID NOT NULL,
    total NUMERIC(19,4) NOT NULL,
    paid NUMERIC(19,4) NOT NULL,
    balance NUMERIC(19,4) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    CONSTRAINT fk_accounts_receivable_sale
        FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
    CONSTRAINT fk_accounts_receivable_client
        FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_accounts_receivable_status
    ON accounts_receivable(status);
CREATE INDEX IF NOT EXISTS idx_accounts_receivable_due_date
    ON accounts_receivable(due_date);
