#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

curl -sS -X POST "${BASE_URL}/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "firstname": "Demo",
    "lastname": "User",
    "email": "demo.user@dismal.local",
    "password": "Demo123!"
  }'

echo
