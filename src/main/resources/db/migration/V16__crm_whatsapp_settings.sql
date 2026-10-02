ALTER TABLE IF EXISTS integration_settings
    ADD COLUMN IF NOT EXISTS crm_whatsapp_enabled BOOLEAN,
    ADD COLUMN IF NOT EXISTS crm_whatsapp_default_id VARCHAR(32),
    ADD COLUMN IF NOT EXISTS crm_whatsapp_id_by_country TEXT;
