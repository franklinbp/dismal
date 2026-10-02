#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
CLIENT_ID="${CLIENT_ID:?Set CLIENT_ID}"
SOFTWARE_ID="${SOFTWARE_ID:?Set SOFTWARE_ID}"
QUANTITY="${QUANTITY:-1}"
UNIT_PRICE_JSON="null"

if [ -n "${UNIT_PRICE:-}" ]; then
  UNIT_PRICE_JSON="${UNIT_PRICE}"
fi

TOKEN="$(./scripts/02_login.sh | tail -n 1)"

curl -sS -X POST "${BASE_URL}/api/v1/sales" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d "{
    \"clientId\": \"${CLIENT_ID}\",
    \"saleType\": \"CASH\",
    \"items\": [
      {
        \"softwareId\": \"${SOFTWARE_ID}\",
        \"quantity\": ${QUANTITY},
        \"unitPrice\": ${UNIT_PRICE_JSON}
      }
    ]
  }"
