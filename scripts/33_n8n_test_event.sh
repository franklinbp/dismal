#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TO="${TO:-}"
MESSAGE="${MESSAGE:-hello}"

TOKEN="$(./scripts/02_login.sh | tail -n 1)"

curl -sS -X POST "${BASE_URL}/api/v1/integrations/n8n/test" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d "{
    \"to\": \"${TO}\",
    \"message\": \"${MESSAGE}\"
  }"
