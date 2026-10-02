#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
SALE_ID="${SALE_ID:?Set SALE_ID}"

TOKEN="$(./scripts/02_login.sh | tail -n 1)"

curl -sS -X POST "${BASE_URL}/api/v1/sales/${SALE_ID}/confirm" \
  -H "Authorization: Bearer ${TOKEN}"
