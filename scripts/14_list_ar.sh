#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

TOKEN="$(./scripts/02_login.sh | tail -n 1)"

curl -sS -X GET "${BASE_URL}/api/v1/ar" \
  -H "Authorization: Bearer ${TOKEN}"
