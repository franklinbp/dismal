#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TOKEN="${TOKEN:-}"

if [ -z "${TOKEN}" ]; then
  echo "TOKEN env var is required. Example:"
  echo "TOKEN=... ./scripts/03_create_product.sh"
  exit 1
fi

curl -sS -X POST "${BASE_URL}/api/v1/store/product" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "name": "Dismal Billing",
    "description": "Facturacion y cobranza.",
    "price": 59.00,
    "platform": "Web",
    "imageUrl": "https://example.com/img/billing.png"
  }'

echo
