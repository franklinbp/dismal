# WooCommerce Integration API

## Endpoint

`POST /api/public/integrations/woocommerce/order-paid`

## Security

Send the header:

`X-Dismal-Integration-Key: <WOOCOMMERCE_API_KEY>`

Configure the backend with:

`WOOCOMMERCE_API_KEY=your-shared-secret`

## Purpose

Receives a paid WooCommerce order, creates or updates the customer, creates the sale, confirms it, records the payment and stores an idempotency record using the external WooCommerce order ID.

## Idempotency

The backend stores `orderId` in `woo_order_sync`.

If WooCommerce retries the same paid order, the backend returns the existing `saleId` with `duplicated=true`.

## Request body

```json
{
  "orderId": "woo-123",
  "orderNumber": "123",
  "firstName": "Ana",
  "lastName": "Cliente",
  "email": "ana@example.com",
  "phone": "0999999999",
  "taxId": "1234567890",
  "customerType": "FINAL",
  "paymentMethod": "TRANSFER",
  "paymentReference": "txn-123",
  "totalPaid": 99.00,
  "items": [
    {
      "softwareId": "11111111-1111-1111-1111-111111111111",
      "quantity": 1,
      "unitPrice": 99.00
    }
  ]
}
```

## Response

```json
{
  "externalOrderId": "woo-123",
  "saleId": "22222222-2222-2222-2222-222222222222",
  "status": "CONFIRMED",
  "duplicated": false
}
```

## WordPress / WooCommerce flow

1. WooCommerce marks the order as paid.
2. Your plugin sends the payload to this endpoint.
3. Dismal creates and confirms the sale.
4. Dismal records the payment.
5. Dismal assigns licenses through the existing sale flow.
6. Store `saleId` in WooCommerce order meta for traceability.

## Recommended WooCommerce order meta

- `_dismal_external_order_id`
- `_dismal_sale_id`
- `_dismal_sync_status`

