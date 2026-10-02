UPDATE price_lists
SET type = 'EC_FINAL',
    name = 'Ecuador - Cliente final'
WHERE type = 'PUBLIC';

UPDATE price_lists
SET type = 'EC_DISTRIBUTOR',
    name = 'Ecuador - Distribuidor'
WHERE type = 'DISTRIBUTOR';

INSERT INTO price_lists (id, type, name, enabled)
SELECT '33333333-3333-3333-3333-333333333333', 'PE_FINAL', 'Peru - Cliente final', TRUE
WHERE NOT EXISTS (SELECT 1 FROM price_lists WHERE type = 'PE_FINAL');

INSERT INTO price_lists (id, type, name, enabled)
SELECT '44444444-4444-4444-4444-444444444444', 'PE_DISTRIBUTOR', 'Peru - Distribuidor', TRUE
WHERE NOT EXISTS (SELECT 1 FROM price_lists WHERE type = 'PE_DISTRIBUTOR');

INSERT INTO price_list_items (id, price_list_id, software_id, price)
SELECT (
    substr(md5(pl.type || '-' || s.id::text), 1, 8) || '-' ||
    substr(md5(pl.type || '-' || s.id::text), 9, 4) || '-' ||
    substr(md5(pl.type || '-' || s.id::text), 13, 4) || '-' ||
    substr(md5(pl.type || '-' || s.id::text), 17, 4) || '-' ||
    substr(md5(pl.type || '-' || s.id::text), 21, 12)
)::uuid,
pl.id,
s.id,
s.price
FROM price_lists pl
JOIN software s ON 1 = 1
WHERE pl.type IN ('PE_FINAL', 'PE_DISTRIBUTOR')
  AND NOT EXISTS (
      SELECT 1
      FROM price_list_items pli
      WHERE pli.price_list_id = pl.id
        AND pli.software_id = s.id
  );
