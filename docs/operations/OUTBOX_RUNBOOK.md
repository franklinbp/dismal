# Outbox Runbook

## Estados
- PENDING: listo para enviar o reintentar.
- FAILED: error en el envío, espera reintento.
- SENT: entregado exitosamente (2xx).

## Eventos de notificación
- SALE_CONFIRMED
- INVOICE_ISSUED
- PAYMENT_RECEIVED
- LICENSES_DELIVERED
- AR_OVERDUE
- SALE_CANCELLED

## Delivery logs
Cada envío genera registros en `notification_delivery_logs` con:
- `outboxEventId`, `channel` (EMAIL/WHATSAPP), `status` (SENT/FAILED/DELIVERED), `error`.

Si no hay callback del proveedor, el dispatcher guarda SENT/FAILED automáticamente.

## Listar eventos
```bash
BASE_URL="http://localhost:8080" ./scripts/30_outbox_list_failed.sh
```

Listar con filtros:
```
GET /api/v1/integrations/outbox?status=FAILED&eventType=SALE_CONFIRMED&aggregateId=<uuid>&page=0&size=20
```

## Reintentar un evento puntual
```bash
OUTBOX_ID="<uuid>" ./scripts/31_outbox_retry_one.sh
```

## Reintentar todos los FAILED
```bash
./scripts/32_outbox_retry_all_failed.sh
EVENT_TYPE="PAYMENT_RECEIVED" ./scripts/32_outbox_retry_all_failed.sh
```

## Enviar test de integración
```bash
MESSAGE="ping" ./scripts/33_n8n_test_event.sh
```

## Troubleshooting

- n8n caído / DNS / timeout:
  - los eventos quedan en FAILED con `lastError`.
  - reintenta cuando el webhook vuelva a estar disponible.
- Firma inválida:
  - revisa `integrations.n8n.secret` y que n8n valide el HMAC con el cuerpo crudo.
- Respuesta 4xx:
  - revisar `lastResponseBody` (truncado a 2KB) y corregir payload/validación.
- Respuesta 5xx:
  - n8n o API intermediaria con error; reintenta luego.

## Variables de plantillas
Soportadas en NotificationTemplate:
- `{clientName}`, `{invoiceNumber}`, `{total}`, `{balance}`, `{dueDate}`, `{licenses}`, `{pdfUrl}`

## Templates y delivery logs (admin)
- `GET /api/v1/integrations/notifications/templates`
- `POST /api/v1/integrations/notifications/templates`
- `PUT /api/v1/integrations/notifications/templates/{id}`
- `DELETE /api/v1/integrations/notifications/templates/{id}`
- `GET /api/v1/integrations/notifications/templates/delivery-logs?outboxEventId=<uuid>`

Callback (n8n):
```
POST /api/v1/integrations/notifications/templates/delivery-callback
Header: X-Integration-Secret: <integrations.n8n.secret>
Body: { outboxEventId, channel, status, providerMessageId, error }
```
