#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
PAGE="${PAGE:-0}"
SIZE="${SIZE:-20}"

TOKEN="$(./scripts/02_login.sh | tail -n 1)"

curl -sS -X GET "${BASE_URL}/api/v1/integrations/outbox?status=FAILED&page=${PAGE}&size=${SIZE}" \
  -H "Authorization: Bearer ${TOKEN}"
