#!/usr/bin/env bash
set -euo pipefail

WEBHOOK_URL="${WEBHOOK_URL:?Set WEBHOOK_URL}"
EVENT_TYPE="${EVENT_TYPE:-SALE_CONFIRMED}"

if command -v python3 >/dev/null 2>&1; then
  IDEMPOTENCY_KEY="$(python3 - <<'PY'
import uuid
print(uuid.uuid4())
PY
)"
else
  IDEMPOTENCY_KEY="$(uuidgen)"
fi

PAYLOAD="$(cat <<JSON
{
  "eventType": "${EVENT_TYPE}",
  "saleId": "11111111-1111-1111-1111-111111111111",
  "saleType": "CREDIT",
  "status": "CONFIRMED",
  "total": 150.00,
  "currency": "USD",
  "client": {
    "id": "22222222-2222-2222-2222-222222222222",
    "email": "client@example.com",
    "phone": "+593999000111",
    "name": "Juan Perez"
  },
  "items": [
    {
      "softwareId": "33333333-3333-3333-3333-333333333333",
      "name": "Suite Pro",
      "qty": 1,
      "unitPrice": 150.00
    }
  ],
  "ar": {
    "dueDate": "2026-01-10",
    "balance": 150.00
  },
  "templateResolved": {
    "emailSubject": "Tu compra en Dismal",
    "emailBody": "Hola Juan, tu factura esta lista.",
    "whatsappText": "Licencias: LIC-AAA-111, LIC-BBB-222"
  },
  "meta": {
    "timestamp": "2026-01-02T10:15:30",
    "idempotencyKey": "${IDEMPOTENCY_KEY}"
  }
}
JSON
)"

SIGNATURE=""
if [ -n "${N8N_SECRET:-}" ]; then
  SIGNATURE="$(printf '%s' "${PAYLOAD}" | openssl dgst -sha256 -hmac "${N8N_SECRET}" | awk '{print $2}')"
fi

curl -sS -X POST "${WEBHOOK_URL}" \
  -H "Content-Type: application/json" \
  -H "X-Event-Type: ${EVENT_TYPE}" \
  -H "X-Idempotency-Key: ${IDEMPOTENCY_KEY}" \
  ${SIGNATURE:+-H "X-Signature: ${SIGNATURE}"} \
  -d "${PAYLOAD}"
