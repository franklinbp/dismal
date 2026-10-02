ALTER TABLE sales_targets
    ADD COLUMN IF NOT EXISTS status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE sales_targets
    ADD COLUMN IF NOT EXISTS period_start DATE;

ALTER TABLE sales_targets
    ADD COLUMN IF NOT EXISTS period_end DATE;

ALTER TABLE sales_targets
    ADD COLUMN IF NOT EXISTS closed_at TIMESTAMP WITH TIME ZONE;

UPDATE sales_targets
SET period_end = COALESCE(period_end, deadline)
WHERE period_end IS NULL
  AND deadline IS NOT NULL;

UPDATE sales_targets
SET period_start = COALESCE(
        period_start,
        date_trunc('quarter', COALESCE(deadline, CURRENT_DATE))::date
    ),
    period_end = COALESCE(
        period_end,
        (date_trunc('quarter', COALESCE(deadline, CURRENT_DATE))::date + interval '3 months - 1 day')::date
    ),
    status = COALESCE(status, 'ACTIVE')
WHERE period_start IS NULL
   OR period_end IS NULL
   OR status IS NULL;

CREATE INDEX IF NOT EXISTS idx_sales_targets_status
    ON sales_targets (status);

CREATE INDEX IF NOT EXISTS idx_sales_targets_status_software
    ON sales_targets (status, software_id);

CREATE INDEX IF NOT EXISTS idx_sales_targets_period
    ON sales_targets (period_start, period_end);
