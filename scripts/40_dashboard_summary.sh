#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TOKEN="${TOKEN:-$(./scripts/02_login.sh | tail -n 1)}"

curl -sS -X GET "${BASE_URL}/api/v1/dashboard/admin/summary" \
  -H "Authorization: Bearer ${TOKEN}"
