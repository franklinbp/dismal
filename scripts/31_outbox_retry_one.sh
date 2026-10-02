#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
OUTBOX_ID="${OUTBOX_ID:?Set OUTBOX_ID}"

TOKEN="$(./scripts/02_login.sh | tail -n 1)"

curl -sS -X POST "${BASE_URL}/api/v1/integrations/outbox/${OUTBOX_ID}/retry" \
  -H "Authorization: Bearer ${TOKEN}"
