# Dismal Apps - Integracion y Conexion (Web, Android, Desktop)

## Objetivo
Este documento describe lo necesario para que las 3 aplicaciones (Web, Android y Desktop) funcionen correctamente contra el backend Dismal, usando el mismo API y las mismas reglas de negocio.

## 1) Backend (Base del sistema)
- Servicio: Spring Boot (Java 21)
- API base: `http://localhost:8080`
- Autenticacion: JWT
- Config clave en `src/main/resources/application.properties`
  - `application.security.jwt.secret-key`
  - `application.security.jwt.expiration`
  - `data.encryption.aes-key`
  - `integrations.n8n.webhook-url`
  - `app.sales.fixed-cost-global`
  - `app.sales.fixed-cost-mode`

### Endpoints principales
Auth:
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/authenticate`

Store:
- `POST /api/v1/store/product`
- `GET /api/v1/store/products`
- `POST /api/v1/store/license`
- `PATCH /api/v1/store/product/{id}/price`
- `DELETE /api/v1/store/license/{id}/deactivate`

Customers:
- `GET /api/v1/customers`
- `GET /api/v1/customers/summary`
- `PUT /api/v1/customers/{id}`
- `DELETE /api/v1/customers/{id}`

Admin (Usuarios internos y clientes):
- `GET /api/v1/admin/users?page=&size=&q=&type=internal|client`
- `POST /api/v1/admin/users`
- `PUT /api/v1/admin/users/{id}`
- `PATCH /api/v1/admin/users/{id}/status` (soft delete / activar)
- `POST /api/v1/admin/users/{id}/reset-password` (solo ADMIN, DEV)

Marketing:
- `POST /api/v1/marketing/campaigns`
- `GET /api/v1/marketing/campaigns`

Reports:
- `GET /api/v1/reports/sales`
- `GET /api/v1/reports/profit`
- `GET /api/v1/reports/inactive-customers`
- `GET /api/v1/reports/inventory-alerts`

Sales:
- `POST /api/v1/sales/purchase` (propuesto, TODO backend)

Sales Targets (Planeacion):
- `POST /api/v1/sales-targets`
- `PUT /api/v1/sales-targets/{id}`
- `DELETE /api/v1/sales-targets/{id}`
- `GET /api/v1/sales-targets`
- `GET /api/v1/sales-targets/{id}`
- `GET /api/v1/sales-targets/summary`

## 2) Requerimientos comunes para las 3 apps
### Autenticacion
- Todas las apps deben guardar el token JWT tras login.
- El token se envia en header:
  - `Authorization: Bearer <TOKEN>`

### Almacenamiento seguro del JWT
- Web: memoria + refresh en backend (TODO). Evitar `localStorage` si es posible; preferir cookies `HttpOnly` si se agrega backend de sesion.
- Android: `EncryptedSharedPreferences` o `DataStore` cifrado.
- Desktop (JavaFX): almacenamiento cifrado local (archivo cifrado o keystore del OS).

### Configuracion por entorno
- Base URL configurable:
  - Dev: `http://localhost:8080`
  - Produccion: `https://TU_DOMINIO`
- Cada app debe permitir cambiar o inyectar la URL base.

### Manejo de errores
- API devuelve errores con estructura:
  - `timestamp`, `status`, `error`, `message`, `path`
- Mostrar mensajes amigables y no exponer detalles internos.
- Contrato esperado (HTTP):
  - 401: token invalido/expirado.
  - 403: autenticado sin permisos (cuando se implemente por rol).
  - 404: recurso no encontrado.
  - 409: conflicto (ej. sin stock).
  - 402: credito insuficiente.
  - 500: error inesperado.

### Licencias multi-activacion
- Una licencia puede venderse varias veces (hasta `maxActivations`).
- Cada compra consume 1 activacion.

### Planeacion vs ventas reales
- Metas (sales-targets) sirven para planeacion financiera y no crean ordenes.
- No afectan inventario ni ventas reales.

### Soft delete (usuarios/clientes)
- No se eliminan fisicamente; se cambia `enabled=false`.
- El ultimo ADMIN no puede ser desactivado.

## API Error Contract
### JSON shape
```
{
  "timestamp": "2024-01-01T10:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "No stock available for software ID: ...",
  "path": "/api/v1/sales/purchase"
}
```

### Ejemplos
Validation error (400):
```
{
  "timestamp": "2024-01-01T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "validation failed",
  "path": "/api/v1/auth/register"
}
```

Unauthorized (401):
```
{
  "timestamp": "2024-01-01T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/v1/reports/sales"
}
```

Forbidden (403):
```
{
  "timestamp": "2024-01-01T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access is denied",
  "path": "/api/v1/customers"
}
```

Conflict (409 - no stock):
```
{
  "timestamp": "2024-01-01T10:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "No stock available for software ID: ...",
  "path": "/api/v1/sales/purchase"
}
```

Server error (500):
```
{
  "timestamp": "2024-01-01T10:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred. Please try again later.",
  "path": "/api/v1/store/product"
}
```

### Guidance para frontend
- Mostrar mensajes amigables al usuario final.
- No exponer detalles internos ni stack traces.
- Registrar errores tecnicos solo en logs locales/observabilidad.

## Validacion (expectativas)
- Usar `@Valid` en DTOs y constraints por campo (best practice).
- Auth/Register: `firstname`, `lastname`, `email`, `password` requeridos.
- Auth/Login: `email`, `password` requeridos.
- Store/Product: `name`, `price` requeridos.
- Store/License: `softwareId`, `licenseKey`, `purchasePrice`, `maxActivations` requeridos.
- Sales/Purchase (propuesto): `softwareId`, `userId` requeridos.

## Contrato API: Compra publica (propuesta)
### Endpoint
- `POST /api/v1/sales/purchase` (TODO: controller publico)

### Request JSON
```
{
  "softwareId": "uuid-del-software",
  "userId": "uuid-del-cliente"
}
```

## Contrato API: Metas y rentabilidad (planeacion)
### Formulas usadas
- marginUnit = salePrice - variableCost
- targetRevenue = metaUnits * salePrice
- variableCostTotal = metaUnits * variableCost
- contributionTotal = metaUnits * marginUnit
- breakEvenUnits = ceil(fixedCost / marginUnit) si marginUnit > 0
- breakEvenRevenue = breakEvenUnits * salePrice
- expectedProfit = contributionTotal - fixedCost
- profitable = marginUnit > 0
- targetAchieved = unitsSoldCurrent >= metaUnits

### Request JSON (crear meta)
```
{
  "softwareId": "uuid-del-software",
  "metaUnits": 100,
  "salePrice": 25.00,
  "variableCost": 8.00,
  "fixedCostProduct": 120.00,
  "unitsSoldCurrent": 20,
  "deadline": "2026-02-01",
  "notes": "Meta Q1"
}
```

### Response JSON (ejemplo)
```
{
  "id": "uuid-meta",
  "softwareId": "uuid-del-software",
  "softwareName": "Suite Pro",
  "metaUnits": 100,
  "salePrice": 25.00,
  "variableCost": 8.00,
  "fixedCostProduct": 120.00,
  "unitsSoldCurrent": 20,
  "deadline": "2026-02-01",
  "notes": "Meta Q1",
  "marginUnit": 17.00,
  "targetRevenue": 2500.00,
  "variableCostTotal": 800.00,
  "contributionTotal": 1700.00,
  "breakEvenUnits": 8,
  "breakEvenRevenue": 200.00,
  "expectedProfit": 1580.00,
  "profitable": true,
  "targetAchieved": false,
  "fixedCostApplied": 120.00,
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```
### Response JSON (200)
```
{
  "orderId": "uuid-orden",
  "softwareId": "uuid-del-software",
  "userId": "uuid-del-cliente",
  "salePrice": 99.99,
  "purchaseDate": "2024-01-01T10:30:00"
}
```

### Errores esperados
- 400: request invalido (campos nulos, UUID invalidos).
- 401: sin token o token invalido.
- 403: sin permisos (cuando se implemente por rol).
- 409: sin stock (no hay licencias ACTIVAS con activaciones disponibles).
- 500: error inesperado.

### Idempotencia
- Recomendado: header `Idempotency-Key` para evitar compras duplicadas.
- TODO backend: almacenar claves y responder el mismo resultado en reintentos.

## Como validar backend sin UI
- Swagger UI: `http://localhost:8080/swagger-ui/index.html` (o `/swagger-ui/`).
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`.
- Scripts curl: ejecutar desde `scripts/`:
  - `01_register.sh` (opcional).
  - `02_login.sh` (imprime token).
  - `03_create_product.sh` (requiere `TOKEN=...`).
  - `04_list_products.sh` (requiere `TOKEN=...`).
- Postman: importar la coleccion desde OpenAPI y probar endpoints.
- Run en dev:
  - CLI: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
  - Env: `SPRING_PROFILES_ACTIVE=dev` o `DISMAL_SEED=true`
- Android emulator:
  - Usar `http://10.0.2.2:8080` como base URL.
- Troubleshooting auth 500:
  - AES key invalida (longitud incorrecta).
  - JWT secret faltante o mal formateado (base64).
  - Perfil equivocado (dev no activo).

## 3) Web (Next.js)
### Objetivo
- Panel administrativo: productos, licencias, clientes, reportes, marketing.

### Requisitos tecnicos
- Next.js + React
- Cliente API centralizado (fetch/axios)
- Guardar token en memoria/secure storage

### Flujos clave
- Login -> Token
- CRUD productos/licencias
- Reportes y campanas

## 4) Android (Kotlin + Jetpack Compose)
### Objetivo
- App movil para operaciones basicas: login, catalogo, compras, consultas de estado.

### Requisitos tecnicos
- Kotlin + Compose
- HTTP client (Ktor/Retrofit)
- Almacenar token en `EncryptedSharedPreferences`

### Flujos clave
- Login -> Token
- Listado de software
- Compra de software
- Ver ordenes / historial (cuando exista endpoint)

## 5) Desktop (JavaFX)
### Objetivo
- Escritorio para operaciones internas (ventas, inventario, reportes).

### Requisitos tecnicos
- JavaFX
- Cliente HTTP (HttpClient)
- Almacenar token en memoria o archivo cifrado

### Flujos clave
- Login -> Token
- Gestion de productos/licencias
- Reportes

## 6) Paso a paso para que funcione todo
1) Backend corriendo en `localhost:8080`.
2) Crear usuarios en `/api/v1/auth/register`.
3) Login y guardar JWT.
4) Conectar apps a la base URL correcta.
5) Consumir endpoints con `Authorization: Bearer <token>`.
6) Verificar flujos: productos -> licencias -> ventas -> reportes.

## 7) Paso siguiente recomendado
- Definir un SDK/cliente API comun (TS para web, Kotlin para Android, Java para Desktop).
- Normalizar modelos de datos y estados.
- Definir UI minima (MVP) por plataforma.

## Estrategia profesional de consumo de API
### OpenAPI como fuente de verdad
- Definir el contrato de endpoints y esquemas en OpenAPI (source of truth).
- Mantener versionado el spec junto al backend.

### Generacion de clientes
- Web: cliente TypeScript generado en `packages/shared`.
- Android: cliente Kotlin generado en `apps/android`.
- Desktop: cliente Java generado en `apps/desktop`.

### Versionado
- Versionar API con prefijo (`/api/v1`) y versionar clientes en sync con el backend.
- Recomendado: semver para clientes (`major` cuando se rompa compatibilidad).

### Ubicacion del codigo generado
- `packages/shared/src/generated` (TypeScript)
- `apps/android/app/src/main/java/.../generated`
- `apps/desktop/src/main/java/.../generated`
- No editar manualmente el codigo generado; regenerar desde OpenAPI.
