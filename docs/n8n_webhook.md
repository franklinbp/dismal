# n8n Webhook Integration (Sales & Notifications)

## Event catalog
- SALE_CONFIRMED
- INVOICE_ISSUED
- PAYMENT_RECEIVED
- LICENSES_DELIVERED
- AR_OVERDUE
- SALE_CANCELLED

## Payload examples

### LICENSES_DELIVERED (envio inmediato)
```json
{
  "eventType": "LICENSES_DELIVERED",
  "saleId": "11111111-1111-1111-1111-111111111111",
  "saleType": "CASH",
  "paymentType": "CASH",
  "status": "CONFIRMED",
  "total": 150.00,
  "currency": "USD",
  "sale": {
    "id": "11111111-1111-1111-1111-111111111111",
    "saleType": "CASH",
    "status": "CONFIRMED",
    "total": 150.00,
    "currency": "USD"
  },
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
    "dueDate": null,
    "balance": 0
  },
  "invoice": {
    "number": "INV-20260102-0001",
    "pdfUrl": "https://files.dismal.com/invoices/11111111-1111-1111-1111-111111111111"
  },
  "licenses": [
    "LIC-AAA-111"
  ],
  "licensesDelivered": [
    "LIC-AAA-111"
  ],
  "licensesDetailed": [
    {
      "licenseId": "44444444-4444-4444-4444-444444444444",
      "licenseKey": "LIC-AAA-111",
      "maxActivations": 3,
      "usedActivations": 0,
      "softwareId": "33333333-3333-3333-3333-333333333333",
      "softwareName": "Suite Pro"
    }
  ],
  "templateResolved": {
    "emailSubject": "Tus licencias digitales",
    "emailBody": "Hola Juan, aqui estan tus licencias: Suite Pro: LIC-AAA-111",
    "whatsappText": "Licencias: Suite Pro: LIC-AAA-111"
  },
  "meta": {
    "timestamp": "2026-01-02T10:15:30",
    "idempotencyKey": "55555555-5555-5555-5555-555555555555"
  }
}
```

### SALE_CONFIRMED (credit sale)
```json
{
  "eventType": "SALE_CONFIRMED",
  "saleId": "11111111-1111-1111-1111-111111111111",
  "saleType": "CREDIT",
  "paymentType": "CREDIT",
  "status": "CONFIRMED",
  "total": 150.00,
  "currency": "USD",
  "sale": {
    "id": "11111111-1111-1111-1111-111111111111",
    "saleType": "CREDIT",
    "status": "CONFIRMED",
    "total": 150.00,
    "currency": "USD"
  },
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
  "invoice": {
    "number": "INV-20260102-0001",
    "pdfUrl": "https://files.dismal.com/invoices/11111111-1111-1111-1111-111111111111"
  },
  "templateResolved": {
    "emailSubject": "Tu compra en Dismal",
    "emailBody": "Hola Juan, tu factura INV-20260102-0001 esta lista.",
    "whatsappText": "Compra confirmada. Te enviaremos tus licencias en breve."
  },
  "meta": {
    "timestamp": "2026-01-02T10:15:30",
    "idempotencyKey": "44444444-4444-4444-4444-444444444444"
  }
}
```

### PAYMENT_RECEIVED (cash sale)
```json
{
  "eventType": "PAYMENT_RECEIVED",
  "saleId": "55555555-5555-5555-5555-555555555555",
  "saleType": "CASH",
  "paymentType": "CASH",
  "status": "PAID",
  "total": 80.00,
  "currency": "USD",
  "sale": {
    "id": "55555555-5555-5555-5555-555555555555",
    "saleType": "CASH",
    "status": "PAID",
    "total": 80.00,
    "currency": "USD"
  },
  "client": {
    "id": "66666666-6666-6666-6666-666666666666",
    "email": "cash@example.com",
    "phone": "+593999000222",
    "name": "Ana Lima"
  },
  "items": [
    {
      "softwareId": "77777777-7777-7777-7777-777777777777",
      "name": "Editor Plus",
      "qty": 2,
      "unitPrice": 40.00
    }
  ],
  "ar": {
    "dueDate": null,
    "balance": 0
  },
  "payment": {
    "paymentId": "77777777-7777-7777-7777-777777777777",
    "amount": 80.00,
    "method": "CASH",
    "reference": "CASH-0001",
    "createdAt": "2026-01-02T10:20:05"
  },
  "templateResolved": {
    "emailSubject": "Pago recibido",
    "emailBody": "Pago recibido por 80.00 USD.",
    "whatsappText": "Pago recibido. Gracias."
  },
  "meta": {
    "timestamp": "2026-01-02T10:20:10",
    "idempotencyKey": "88888888-8888-8888-8888-888888888888"
  }
}
```

### AR_OVERDUE
```json
{
  "eventType": "AR_OVERDUE",
  "client": {
    "id": "99999999-9999-9999-9999-999999999999",
    "email": "client@example.com",
    "phone": "+593999000333",
    "name": "Maria Perez"
  },
  "ar": {
    "balance": 220.00,
    "daysOverdue": 12,
    "dueDate": "2026-01-20"
  },
  "meta": {
    "timestamp": "2026-02-01T02:10:00",
    "idempotencyKey": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
  }
}
```

## Signature validation (HMAC-SHA256)

Server sends header `X-Signature` computed as:

```
hex(hmac_sha256(payload_json, integrations.n8n.secret))
```

Example (Node.js):
```js
const crypto = require("crypto");
const signature = crypto
  .createHmac("sha256", process.env.N8N_SECRET)
  .update(rawBody, "utf8")
  .digest("hex");
```

Compare with `X-Signature`. The raw payload string must match exactly what was received.

## Test with curl

```bash
export WEBHOOK_URL="https://your-n8n-instance.com/webhook/test-webhook"
export N8N_SECRET="your-secret"
./scripts/20_test_n8n_webhook.sh
```

## n8n flujo recomendado (LICENSES_DELIVERED)

1) **Webhook (entrada)**  
   - Metodo: POST  
   - Validar firma `X-Signature` con `N8N_SECRET` (opcional si ya validas en n8n).  
2) **Switch** por `eventType` = `LICENSES_DELIVERED`.  
3) **Set / Function**: construir mensajes con `templateResolved` y fallback.  
4) **HTTP Request** a Evolution API (WhatsApp).  
5) **SMTP** para Gmail (Email).  
6) **HTTP Request** al callback de delivery (`/api/v1/integrations/notifications/templates/delivery-callback`).

### Mensajes (recomendado)
- Email: usar `templateResolved.emailSubject` y `templateResolved.emailBody`.  
- WhatsApp: usar `templateResolved.whatsappText`.  

Si no existe `templateResolved`, usar fallback con `licensesDetailed` o `licenses`.

### Evolution API (WhatsApp) - ejemplo
```http
POST /message/sendText
Authorization: Bearer <EVOLUTION_TOKEN>
Content-Type: application/json

{
  "number": "+593999000111",
  "text": "Licencias: Suite Pro: LIC-AAA-111"
}
```

### Gmail SMTP (n8n)
- Host: `smtp.gmail.com`
- Port: `465` (SSL) o `587` (TLS)
- User: `SMTP_USER`
- Password: `SMTP_PASSWORD` (App Password recomendado)
- From: `SMTP_FROM_EMAIL`

## Templates sugeridos (LICENSES_DELIVERED)

### Email (subject/body)
```json
{
  "eventType": "LICENSES_DELIVERED",
  "channel": "EMAIL",
  "subject": "Tus licencias digitales de Dismal",
  "body": "Hola {clientName},\\n\\nGracias por tu compra. Aqui estan tus licencias:\\n{licenses}\\n\\nFactura: {invoiceNumber}\\nPDF: {pdfUrl}\\n\\nSoporte: soporte@dismal.com",
  "enabled": true
}
```

### WhatsApp (body)
```json
{
  "eventType": "LICENSES_DELIVERED",
  "channel": "WHATSAPP",
  "subject": null,
  "body": "Hola {clientName}. Tus licencias: {licenses}. Factura: {invoiceNumber}. PDF: {pdfUrl}",
  "enabled": true
}
```
