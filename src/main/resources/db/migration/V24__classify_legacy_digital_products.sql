-- Products inherited from the digital-only system can be identified by their licenses.
UPDATE software s
SET physical_product = FALSE
WHERE EXISTS (SELECT 1 FROM licenses l WHERE l.software_id = s.id);

ALTER TABLE software ALTER COLUMN physical_product SET DEFAULT FALSE;
