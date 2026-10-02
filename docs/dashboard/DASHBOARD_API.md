# Dashboard API (Admin/Manager)

## Endpoints
- `GET /api/v1/dashboard/admin/summary`
- `GET /api/v1/dashboard/admin/recent-sales?from=YYYY-MM-DD&to=YYYY-MM-DD&page=0&size=20`
- `GET /api/v1/dashboard/admin/ar?status=OPEN|OVERDUE&from=YYYY-MM-DD&to=YYYY-MM-DD&page=0&size=20`
- `GET /api/v1/dashboard/admin/sales-targets?sort=profit|margin|deadline&from=YYYY-MM-DD&to=YYYY-MM-DD&page=0&size=20`
- `GET /api/v1/dashboard/admin/outbox?status=FAILED|PENDING|SENT&from=YYYY-MM-DD&to=YYYY-MM-DD&page=0&size=20`

## Roles permitidos
- ADMIN, MANAGER
- USER: sin acceso

## Summary (ejemplo)
```json
{
  "todaySalesCount": 3,
  "todaySalesAmount": 240.00,
  "monthSalesCount": 18,
  "monthSalesAmount": 1450.00,
  "monthExpectedProfit": 520.00,
  "overdueArCount": 2,
  "overdueArBalance": 300.00,
  "openArCount": 5,
  "openArBalance": 820.00,
  "nonProfitableProductsCount": 1,
  "outboxFailedCount": 4
}
```

## Recent sales (ejemplo con paginacion)
```json
{
  "content": [
    {
      "id": "uuid",
      "date": "2026-01-02T10:30:00",
      "clientName": "Ana Lima",
      "clientEmail": "ana@example.com",
      "saleType": "CASH",
      "status": "PAID",
      "total": 80.00,
      "paid": 80.00,
      "balance": 0.00
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

## Accounts receivable (ejemplo con paginacion)
```json
{
  "content": [
    {
      "id": "uuid",
      "clientName": "Juan Perez",
      "clientEmail": "juan@example.com",
      "dueDate": "2026-01-15",
      "total": 150.00,
      "paid": 20.00,
      "balance": 130.00,
      "status": "OPEN"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

## Sales targets (ejemplo con paginacion)
```json
{
  "content": [
    {
      "id": "uuid",
      "productName": "Suite Pro",
      "metaUnits": 100,
      "unitsSoldCurrent": 20,
      "marginUnit": 17.00,
      "expectedProfit": 1580.00,
      "breakEvenUnits": 8,
      "deadline": "2026-02-01",
      "profitable": true,
      "targetAchieved": false
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

## Outbox (ejemplo con paginacion)
```json
{
  "content": [
    {
      "id": "uuid",
      "eventType": "sale.confirmed",
      "aggregateId": "uuid",
      "status": "FAILED",
      "attempts": 3,
      "lastError": "Non-2xx status",
      "createdAt": "2026-01-02T10:00:00Z",
      "nextAttemptAt": "2026-01-02T10:15:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

## Notas de performance
- KPIs calculados con agregaciones (count/sum) en DB.
- Listas paginadas con `page` y `size` (default 0/20).
- Sales targets con sort por margen/ganancia estimada y deadline.
- `monthExpectedProfit` suma expectedProfit de metas con deadline en el mes actual.
