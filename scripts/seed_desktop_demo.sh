#!/usr/bin/env bash
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080}"
EMAIL="${EMAIL:-admin@dismal.local}"
PASSWORD="${PASSWORD:-Admin123!}"

auth_resp="$(curl -sS "${API_BASE}/api/v1/auth/authenticate" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\"}")"

token="$(python3 - <<'PY' <<<"$auth_resp"
import json,sys
try:
    data=json.load(sys.stdin)
    print(data.get("token",""))
except Exception:
    print("")
PY
)"

if [ -z "$token" ]; then
  echo "No token. Verifica EMAIL/PASSWORD o si el backend esta arriba."
  exit 1
fi

products_resp="$(curl -sS "${API_BASE}/api/v1/store/products" \
  -H "Authorization: Bearer ${token}")"

product_count="$(python3 - <<'PY' <<<"$products_resp"
import json,sys
try:
    data=json.load(sys.stdin)
    print(len(data))
except Exception:
    print(0)
PY
)"

if [ "$product_count" -eq 0 ]; then
  echo "Sin productos. Creando ejemplos..."
  curl -sS "${API_BASE}/api/v1/store/products" \
    -H "Authorization: Bearer ${token}" \
    -H "Content-Type: application/json" \
    -d '{"name":"Dismal CRM","description":"CRM para gestion de clientes y ventas.","price":49.00,"platform":"Web","imageUrl":"https://example.com/img/crm.png"}' >/dev/null
  curl -sS "${API_BASE}/api/v1/store/products" \
    -H "Authorization: Bearer ${token}" \
    -H "Content-Type: application/json" \
    -d '{"name":"Dismal POS","description":"Punto de venta para retail con inventario.","price":79.00,"platform":"Windows","imageUrl":"https://example.com/img/pos.png"}' >/dev/null
  curl -sS "${API_BASE}/api/v1/store/products" \
    -H "Authorization: Bearer ${token}" \
    -H "Content-Type: application/json" \
    -d '{"name":"Dismal Analytics","description":"Analitica y reportes avanzados.","price":99.00,"platform":"Web","imageUrl":"https://example.com/img/analytics.png"}' >/dev/null

  products_resp="$(curl -sS "${API_BASE}/api/v1/store/products" \
    -H "Authorization: Bearer ${token}")"
fi

targets_resp="$(curl -sS "${API_BASE}/api/v1/sales-targets" \
  -H "Authorization: Bearer ${token}")"

target_count="$(python3 - <<'PY' <<<"$targets_resp"
import json,sys
try:
    data=json.load(sys.stdin)
    print(len(data))
except Exception:
    print(0)
PY
)"

if [ "$target_count" -eq 0 ]; then
  echo "Sin metas. Creando metas de ejemplo..."
  python3 - <<'PY' <<<"$products_resp" | while IFS= read -r payload; do
import json,sys,datetime
products=json.load(sys.stdin)
deadline=(datetime.date.today()+datetime.timedelta(days=45)).isoformat()
for idx,p in enumerate(products, start=1):
    price=float(p.get("price") or 0)
    variable=round(price*0.4, 2) if price else 0
    payload={
        "softwareId": p.get("id"),
        "metaUnits": 50*idx,
        "salePrice": price,
        "variableCost": variable,
        "fixedCostProduct": None,
        "unitsSoldCurrent": 0,
        "deadline": deadline,
        "notes": "Meta inicial"
    }
    print(json.dumps(payload))
PY
  done | while IFS= read -r payload; do
    curl -sS "${API_BASE}/api/v1/sales-targets" \
      -H "Authorization: Bearer ${token}" \
      -H "Content-Type: application/json" \
      -d "$payload" >/dev/null
  done
fi

echo "Listo. Productos y metas disponibles para la app desktop."
