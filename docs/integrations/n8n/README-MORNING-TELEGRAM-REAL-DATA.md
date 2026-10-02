# Morning Telegram Real Data

Workflow recomendado para enviar a Telegram un resumen diario usando solo datos reales de `Dismal`.

Archivo:

- `dismal-morning-telegram-real-data.json`

## Que consume

1. `POST /api/v1/auth/authenticate`
2. `GET /api/v1/dashboard/admin/summary`
3. `GET /api/v1/reports/sales/daily`
4. `GET /api/v1/reports/sales/monthly`
5. `GET /api/v1/reports/sales/vs-targets`
6. `GET /api/v1/sales-targets/summary`
7. `GET /api/v1/sales-targets`
8. `GET /api/v1/reports/inventory-alerts`
9. `GET /api/v1/ar`
10. `GET /api/v1/expenses`

## Que devuelve en Telegram

- ventas del dia y del mes
- avance real contra metas del sistema
- metas logradas y atrasadas
- productos sin movimiento
- cartera abierta y vencida
- gastos de hoy, vencidos y proximos 3 dias
- prioridad del dia

## Campos que debes cambiar en `Config`

- `email`
- `password`
- `telegramBotToken`
- `telegramChatId`

`baseUrl` ya apunta al endpoint TLS de produccion: `https://api.dismal.vip`.

## Importante

- El workflow queda con `"active": false`; despues de importar, activalo en n8n.
- Guarda el usuario, la contrasena y el token de Telegram como credenciales de n8n; no publiques secretos dentro del JSON.
- Los gastos se calculan con `dueDay` y `monthlyAmount` del endpoint `/api/v1/expenses`.
- La meta mensual ya no es manual: sale de `/api/v1/sales-targets/summary`.
- Telegram es el unico canal de salida en este workflow.
