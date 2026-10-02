#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
OUTBOX_ID="${OUTBOX_ID:?Set OUTBOX_ID}"
TOKEN="${TOKEN:-}"

if [ -z "${TOKEN}" ]; then
  TOKEN="$(./scripts/02_login.sh | tail -n 1)"
fi

curl -sS -X GET "${BASE_URL}/api/v1/integrations/notifications/templates/delivery-logs?outboxEventId=${OUTBOX_ID}" \
  -H "Authorization: Bearer ${TOKEN}"
