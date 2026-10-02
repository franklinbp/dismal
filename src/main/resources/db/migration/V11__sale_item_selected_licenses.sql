ALTER TABLE sale_items
    ADD COLUMN IF NOT EXISTS selected_license_ids TEXT;
