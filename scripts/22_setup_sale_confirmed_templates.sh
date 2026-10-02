#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TOKEN="${TOKEN:-}"

if [ -z "${TOKEN}" ]; then
  TOKEN="$(./scripts/02_login.sh | tail -n 1)"
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is required for JSON escaping." >&2
  exit 1
fi

EMAIL_SUBJECT="Compra confirmada en Dismal"
EMAIL_BODY="Hola {clientName},\n\nConfirmamos tu compra en {companyName}.\n\nResumen de la venta\nVenta: {saleId}\nFecha: {saleDate}\nEstado de pago: {paymentStatus}\nTotal: \${total} USD\n\nProductos\n{items}\n\nSaldo pendiente: \${balance} USD\nFecha de vencimiento: {dueDate}\n\nTus licencias se enviaran en un correo separado cuando queden asignadas.\nSoporte: {supportEmail}\n\nGracias por comprar en {companyName}."
WHATSAPP_BODY="Hola {clientName}, tu compra en {companyName} fue confirmada.\nVenta: {saleId}\nTotal: \${total} USD\nEstado: {paymentStatus}\nSoporte: {supportEmail}"

get_template_id() {
  local channel="$1"
  local response
  response="$(curl -sS -X GET "${BASE_URL}/api/v1/integrations/notifications/templates?eventType=SALE_CONFIRMED&channel=${channel}" \
    -H "Authorization: Bearer ${TOKEN}")"

  python3 -c 'import json,sys
try:
    data=json.loads(sys.argv[1])
    if isinstance(data,list) and data:
        print(data[0].get("id",""))
except Exception:
    pass' "${response}"
}

upsert_template() {
  local channel="$1"
  local subject="$2"
  local body="$3"
  local template_id
  template_id="$(get_template_id "${channel}")"

  if [ -n "${template_id}" ]; then
    curl -sS -X PUT "${BASE_URL}/api/v1/integrations/notifications/templates/${template_id}" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer ${TOKEN}" \
      -d "{
        \"eventType\": \"SALE_CONFIRMED\",
        \"channel\": \"${channel}\",
        \"subject\": ${subject},
        \"body\": ${body},
        \"enabled\": true
      }"
    echo ""
    echo "Updated ${channel} template: ${template_id}"
  else
    curl -sS -X POST "${BASE_URL}/api/v1/integrations/notifications/templates" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer ${TOKEN}" \
      -d "{
        \"eventType\": \"SALE_CONFIRMED\",
        \"channel\": \"${channel}\",
        \"subject\": ${subject},
        \"body\": ${body},
        \"enabled\": true
      }"
    echo ""
    echo "Created ${channel} template"
  fi
}

upsert_template "EMAIL" "\"${EMAIL_SUBJECT}\"" "$(printf '%s' "${EMAIL_BODY}" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))')"
upsert_template "WHATSAPP" "null" "$(printf '%s' "${WHATSAPP_BODY}" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))')"
