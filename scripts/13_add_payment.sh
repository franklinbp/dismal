#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
SALE_ID="${SALE_ID:?Set SALE_ID}"
AMOUNT="${AMOUNT:?Set AMOUNT}"
METHOD="${METHOD:-CASH}"
REFERENCE="${REFERENCE:-}"

TOKEN="$(./scripts/02_login.sh | tail -n 1)"

curl -sS -X POST "${BASE_URL}/api/v1/payments" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d "{
    \"saleId\": \"${SALE_ID}\",
    \"amount\": ${AMOUNT},
    \"method\": \"${METHOD}\",
    \"reference\": \"${REFERENCE}\"
  }"
