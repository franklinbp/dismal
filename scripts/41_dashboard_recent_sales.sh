#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
PAGE="${PAGE:-0}"
SIZE="${SIZE:-20}"
FROM_PARAM=""
TO_PARAM=""
TOKEN="${TOKEN:-$(./scripts/02_login.sh | tail -n 1)}"

if [ -n "${FROM:-}" ]; then
  FROM_PARAM="&from=${FROM}"
fi
if [ -n "${TO:-}" ]; then
  TO_PARAM="&to=${TO}"
fi

curl -sS -X GET "${BASE_URL}/api/v1/dashboard/admin/recent-sales?page=${PAGE}&size=${SIZE}${FROM_PARAM}${TO_PARAM}" \
  -H "Authorization: Bearer ${TOKEN}"
