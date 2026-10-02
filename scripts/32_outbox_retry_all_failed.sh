#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
EVENT_TYPE_QUERY=""

if [ -n "${EVENT_TYPE:-}" ]; then
  EVENT_TYPE_QUERY="&eventType=${EVENT_TYPE}"
fi

TOKEN="$(./scripts/02_login.sh | tail -n 1)"

curl -sS -X POST "${BASE_URL}/api/v1/integrations/outbox/retry-failed?${EVENT_TYPE_QUERY#&}" \
  -H "Authorization: Bearer ${TOKEN}"
