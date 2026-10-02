#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
EMAIL="${EMAIL:-admin@dismal.local}"
PASSWORD="${PASSWORD:-Admin123!}"

response_and_code="$(curl -sS -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/auth/authenticate" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"${EMAIL}\",
    \"password\": \"${PASSWORD}\"
  }")"

response="$(echo "${response_and_code}" | sed '$d')"
status="$(echo "${response_and_code}" | tail -n 1)"

if [ "${status}" != "200" ]; then
  echo "${response}"
  echo "HTTP ${status}" >&2
  exit 1
fi

echo "${response}"

if command -v python3 >/dev/null 2>&1; then
  echo "${response}" | python3 -c "import json,sys; print(json.load(sys.stdin).get('token',''))"
fi
