#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TO="${TO:-}"
SUBJECT="${SUBJECT:-}"
BODY="${BODY:-}"
TOKEN="${TOKEN:-}"

if [ -z "${TOKEN}" ]; then
  TOKEN="$(./scripts/02_login.sh | tail -n 1)"
fi

payload="{}"
if command -v python3 >/dev/null 2>&1; then
  payload="$(python3 - <<'PY'
import json,os
data={}
to=os.environ.get("TO","").strip()
subject=os.environ.get("SUBJECT","").strip()
body=os.environ.get("BODY","").strip()
if to: data["to"]=to
if subject: data["subject"]=subject
if body: data["body"]=body
print(json.dumps(data))
PY
)"
else
  echo "python3 is required for JSON payload." >&2
  exit 1
fi

curl -sS -X POST "${BASE_URL}/api/v1/integrations/settings/test-email" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d "${payload}"
