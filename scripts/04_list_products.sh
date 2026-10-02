#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TOKEN="${TOKEN:-}"

if [ -z "${TOKEN}" ]; then
  echo "TOKEN env var is required. Example:"
  echo "TOKEN=... ./scripts/04_list_products.sh"
  exit 1
fi

curl -sS -X GET "${BASE_URL}/api/v1/store/products" \
  -H "Authorization: Bearer ${TOKEN}"

echo
