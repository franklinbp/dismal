CREATE TABLE IF NOT EXISTS price_lists (
    id UUID PRIMARY KEY,
    type VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS price_list_items (
    id UUID PRIMARY KEY,
    price_list_id UUID NOT NULL,
    software_id UUID NOT NULL,
    price NUMERIC(19,4) NOT NULL,
    CONSTRAINT fk_price_list_items_list
        FOREIGN KEY (price_list_id) REFERENCES price_lists(id) ON DELETE CASCADE,
    CONSTRAINT fk_price_list_items_software
        FOREIGN KEY (software_id) REFERENCES software(id) ON DELETE CASCADE,
    CONSTRAINT uk_price_list_item_list_software UNIQUE (price_list_id, software_id)
);

INSERT INTO price_lists (id, type, name, enabled)
SELECT '11111111-1111-1111-1111-111111111111', 'PUBLIC', 'Lista Publica', TRUE
WHERE NOT EXISTS (SELECT 1 FROM price_lists WHERE type = 'PUBLIC');

INSERT INTO price_lists (id, type, name, enabled)
SELECT '22222222-2222-2222-2222-222222222222', 'DISTRIBUTOR', 'Lista Distribuidor', TRUE
WHERE NOT EXISTS (SELECT 1 FROM price_lists WHERE type = 'DISTRIBUTOR');

INSERT INTO price_list_items (id, price_list_id, software_id, price)
SELECT (
    substr(md5('PUBLIC-' || s.id::text), 1, 8) || '-' ||
    substr(md5('PUBLIC-' || s.id::text), 9, 4) || '-' ||
    substr(md5('PUBLIC-' || s.id::text), 13, 4) || '-' ||
    substr(md5('PUBLIC-' || s.id::text), 17, 4) || '-' ||
    substr(md5('PUBLIC-' || s.id::text), 21, 12)
)::uuid, pl.id, s.id, s.price
FROM price_lists pl
JOIN software s ON 1 = 1
WHERE pl.type = 'PUBLIC'
  AND NOT EXISTS (
      SELECT 1
      FROM price_list_items pli
      WHERE pli.price_list_id = pl.id
        AND pli.software_id = s.id
  );

INSERT INTO price_list_items (id, price_list_id, software_id, price)
SELECT (
    substr(md5('DISTRIBUTOR-' || s.id::text), 1, 8) || '-' ||
    substr(md5('DISTRIBUTOR-' || s.id::text), 9, 4) || '-' ||
    substr(md5('DISTRIBUTOR-' || s.id::text), 13, 4) || '-' ||
    substr(md5('DISTRIBUTOR-' || s.id::text), 17, 4) || '-' ||
    substr(md5('DISTRIBUTOR-' || s.id::text), 21, 12)
)::uuid, pl.id, s.id, s.price
FROM price_lists pl
JOIN software s ON 1 = 1
WHERE pl.type = 'DISTRIBUTOR'
  AND NOT EXISTS (
      SELECT 1
      FROM price_list_items pli
      WHERE pli.price_list_id = pl.id
        AND pli.software_id = s.id
  );
