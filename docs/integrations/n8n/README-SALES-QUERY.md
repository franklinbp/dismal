# n8n Sales Query

Este workflow permite que `n8n` consulte ventas en `Dismal` por API.

Archivo:

- `dismal-sales-query-workflow.json`

## Que hace

1. autentica contra `POST /api/v1/auth/authenticate`
2. usa el `token` JWT recibido
3. consulta `GET /api/v1/reports/sales`
4. devuelve un resumen listo para seguir procesando en `n8n`

## Requisitos

- un usuario `ADMIN` o `MANAGER` en `Dismal`
- `Dismal` accesible desde `n8n`
- rango de fechas valido

## Parametros que debes cambiar

En el nodo `Config`:

- `baseUrl`
- `email`
- `password`
- `startDate`
- `endDate`
- `period`

Ejemplo:

- `baseUrl`: `https://api.dismal.vip`
- `period`: `month`

## Endpoint usado

El reporte requiere estos query params:

- `startDate`
- `endDate`
- `period`

Segun el backend:

- `GET /api/v1/reports/sales`

## Respuesta esperada

El backend devuelve:

- `startDate`
- `endDate`
- `period`
- `dataPoints`

El nodo `Build Summary` agrega:

- `totalBuckets`
- `totalSales`
- `averageSales`
- `topBucket`
- `rawReport`

## Siguiente mejora recomendada

Si quieres automatizar consultas por WhatsApp o Telegram desde `n8n`, el siguiente paso es agregar:

1. un `Webhook Trigger`
2. un nodo `IF` para validar el comando
3. un nodo de salida para responder por el canal que uses
