ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS security_version INTEGER NOT NULL DEFAULT 0;

-- Existing accounts predate email verification. Preserve their access during rollout.
UPDATE users
SET email_verified = TRUE,
    email_verified_at = COALESCE(email_verified_at, CURRENT_TIMESTAMP)
WHERE email_verified = FALSE;

CREATE TABLE IF NOT EXISTS account_action_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(32) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_account_action_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_account_action_tokens_user_type
    ON account_action_tokens(user_id, type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_account_action_tokens_expiry
    ON account_action_tokens(expires_at);

CREATE TABLE IF NOT EXISTS wholesale_applications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    country VARCHAR(2) NOT NULL,
    business_name VARCHAR(180) NOT NULL,
    tax_id VARCHAR(40) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    website VARCHAR(240),
    notes VARCHAR(1000),
    status VARCHAR(24) NOT NULL,
    reviewed_by UUID REFERENCES users(id),
    review_notes VARCHAR(1000),
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_wholesale_applications_user
    ON wholesale_applications(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_wholesale_applications_status
    ON wholesale_applications(status, created_at ASC);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wholesale_applications_pending_user
    ON wholesale_applications(user_id)
    WHERE status = 'PENDING';
