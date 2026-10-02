#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
SALE_ID="${SALE_ID:?Set SALE_ID}"
TOKEN="${TOKEN:-}"

if [ -z "${TOKEN}" ]; then
  TOKEN="$(./scripts/02_login.sh | tail -n 1)"
fi

response="$(curl -sS -X GET "${BASE_URL}/api/v1/integrations/outbox?eventType=LICENSES_DELIVERED&aggregateId=${SALE_ID}&page=0&size=20" \
  -H "Authorization: Bearer ${TOKEN}")"

echo "${response}"

if command -v python3 >/dev/null 2>&1; then
  echo ""
  echo "${response}" | python3 - <<'PY'
import json,sys
try:
  data=json.load(sys.stdin)
  content=data.get("content") or []
  if content:
    print(content[0].get("id",""))
except Exception:
  pass
PY
fi
