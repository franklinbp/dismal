# Dismal - Resumen del Proyecto

## Proposito
Dismal es una API REST para la gestion y venta de licencias de software. Permite administrar productos (software), licencias con multiples activaciones, clientes, ventas, campanas de marketing y reportes operativos/financieros.

## Que hace el proyecto
- Seguridad: autenticacion con JWT, endpoints de registro y login, y proteccion de rutas.
- Store (Inventario): alta y mantenimiento de software, alta de licencias y control de activaciones disponibles.
- Ventas: compra de software consumiendo activaciones de licencias y registro de ordenes historicas.
- Marketing: gestion de campanas y envio programado via webhook (n8n).
- Reportes: ventas por periodo, utilidad neta, clientes inactivos y alertas de inventario frio.

## Modulos principales
- Security
  - Autenticacion: `POST /api/v1/auth/register`, `POST /api/v1/auth/authenticate`.
  - Clientes: `GET /api/v1/customers`, `GET /api/v1/customers/summary`, `PUT /api/v1/customers/{id}`, `DELETE /api/v1/customers/{id}`.
- Store
  - Software: `POST /api/v1/store/product`, `GET /api/v1/store/products`, `PUT /api/v1/store/product/{id}`, `PATCH /api/v1/store/product/{id}/price`, `DELETE /api/v1/store/product/{id}`.
  - Licencias: `POST /api/v1/store/license`, `DELETE /api/v1/store/license/{id}/deactivate`.
  - Metas: `POST /api/v1/store/goals`, `GET /api/v1/store/goals?softwareId=...`.
- Planning (Metas y rentabilidad)
  - Metas: `POST /api/v1/sales-targets`, `PUT /api/v1/sales-targets/{id}`, `DELETE /api/v1/sales-targets/{id}`.
  - Consultas: `GET /api/v1/sales-targets`, `GET /api/v1/sales-targets/{id}`, `GET /api/v1/sales-targets/summary`.
- Sales
  - Compra de software (servicio interno): asigna una licencia disponible y consume una activacion.
  - Eventos: emite `OrderPaidEvent` para integraciones.
- Marketing
  - Campanas: `POST /api/v1/marketing/campaigns`, `GET /api/v1/marketing/campaigns`, `PUT /api/v1/marketing/campaigns/{id}`, `DELETE /api/v1/marketing/campaigns/{id}`.
  - Ejecucion programada: envio cada 15 minutos si la campana esta vencida.
- Reports
  - Ventas: `GET /api/v1/reports/sales`.
  - Utilidad: `GET /api/v1/reports/profit`.
  - Clientes inactivos: `GET /api/v1/reports/inactive-customers`.
  - Inventario frio: `GET /api/v1/reports/inventory-alerts`.

## Security & Roles
- Roles previstos:
  - ADMIN: acceso total a administracion (productos, licencias, clientes, reportes, marketing).
  - MANAGER: acceso a planeacion (metas y rentabilidad) y reportes.
  - OPERATOR (sugerido): operaciones diarias (inventario, ventas). TODO: implementar rol y restricciones.
  - CUSTOMER: acceso a catalogo y compras. TODO: endpoints publicos/cliente para compra/historial.
- Acceso por modulo (estado actual):
  - Publico: `/api/v1/auth/**` y endpoints de Swagger.
  - Protegido con JWT: todo el resto.
  - TODO: agregar reglas por rol en `SecurityConfig` para limitar acceso segun rol.
- Buenas practicas JWT:
  - Expiracion corta (configurable por `application.security.jwt.expiration`).
  - Rotacion y revocacion: sugerir refresh tokens (TODO).
  - Almacenamiento seguro por plataforma (ver `docs/requirements/APPS_INTEGRATION.md`).

## Data handled by the system
- Datos de clientes: nombre, email, telefono, estado `enabled`, datos fiscales y limites de credito.
- Licencias: `licenseKey` se almacena cifrada (AES) en base de datos.
- Ventas/ordenes: historial de compra, software, precio; no se manejan datos de tarjeta.
- Planeacion: metas de ventas y rentabilidad (no impacta ventas reales ni inventario).
- Marketing: campañas (titulo, mensaje, imagen, rol objetivo, estado, fechas).
- Secretos/configuracion: JWT secret, AES key, webhook n8n (deben mantenerse fuera de repositorios publicos).

## Flujo principal (venta)
1) Se solicita una compra de software.
2) Se busca una licencia ACTIVA con activaciones disponibles.
3) Se consume 1 activacion (usedActivations + 1) y se crea una orden.
4) Se publica un evento para integraciones (webhook n8n).

## Planeacion (metas)
- Modulo separado de ventas reales. No descuenta inventario ni crea ordenes.
- Calcula margen unitario, punto de equilibrio y rentabilidad esperada con formulas estandar.

## Compra publica (API)
- Endpoint propuesto: `POST /api/v1/sales/purchase` (ver contrato en `docs/requirements/APPS_INTEGRATION.md`).
- TODO (backend): agregar controller publico que invoque `CheckoutService` y exponga este contrato.

## Configuracion clave
- `application.security.jwt.secret-key` y `application.security.jwt.expiration` para JWT.
- `data.encryption.aes-key` para cifrado de licencias.
- `integrations.n8n.webhook-url` para integraciones de marketing y ventas.
- `app.sales.fixed-cost-global` y `app.sales.fixed-cost-mode` para metas de planeacion.
- Base de datos: PostgreSQL en `src/main/resources/application.properties`.

## Validacion y errores
- Contrato de error estandar: `timestamp`, `status`, `error`, `message`, `path` (ver `docs/requirements/APPS_INTEGRATION.md`).
- Recomendado: usar `@Valid` y constraints en DTOs para validar inputs.

## Production Readiness
- Variables de entorno y secretos: mantener fuera de git y gestionar por entorno (dev/stage/prod).
  - JWT: `application.security.jwt.secret-key`, `application.security.jwt.expiration`.
  - AES: `data.encryption.aes-key`.
  - Integraciones: `integrations.n8n.webhook-url`.
  - DB: `spring.datasource.*`.
- Logging: recomendar logs estructurados (JSON) con request-id/correlation-id.
- Health checks: habilitar Spring Boot Actuator para `/actuator/health` y `/actuator/info`.
- Metrics/tracing: sugerir Prometheus + OpenTelemetry (recomendado, no obligatorio).

## Runbook
### Como correr en local
1) Configurar DB y secrets en `src/main/resources/application.properties` (o variables de entorno).
2) Iniciar PostgreSQL y crear la base `dismal_db`.
3) Ejecutar backend: `./mvnw spring-boot:run`.

### Configurar DB y secretos
1) DB: `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`.
2) JWT: `application.security.jwt.secret-key` (base64) y `application.security.jwt.expiration` (ms).
3) AES: `data.encryption.aes-key` (base64).
4) n8n: `integrations.n8n.webhook-url`.

### Validar flujo de compra (E2E)
1) Crear usuario: `POST /api/v1/auth/register`.
2) Login: `POST /api/v1/auth/authenticate` y guardar JWT.
3) Crear software: `POST /api/v1/store/product`.
4) Crear licencia: `POST /api/v1/store/license` (con `maxActivations` > 0).
5) Ejecutar compra: `POST /api/v1/sales/purchase` (TODO controller publico).
6) Verificar: orden creada y activacion consumida.

### Confirmar webhook n8n
1) Configurar `integrations.n8n.webhook-url` con endpoint real.
2) Ejecutar compra (paso anterior).
3) Verificar en n8n la recepcion del payload.

## Local Bootstrap & Validation
- Semillas DEV: se habilitan solo con `spring.profiles.active=dev` o `DISMAL_SEED=true`.
- Datos creados:
  - Admin: `admin@dismal.local` / `Admin123!` (placeholder).
  - 3 productos de software y licencias con activaciones (si `DISMAL_SEED_LICENSES=true`).
- Seguridad:
  - Cambiar credenciales en entornos reales.
  - No exponer usuarios demo fuera de dev.
- Validacion rapida:
  - `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
  - Usar scripts en `scripts/` o Swagger UI.
- Troubleshooting auth 500:
  - AES key invalida (longitud incorrecta al descifrar licencias).
  - Secretos JWT/AES no configurados o perfil equivocado.

## Testing strategy
- Unit tests (servicios):
  - CheckoutService: seleccion de licencia, consumo de activacion, creacion de orden.
  - CustomerService: filtrado de `enabled`.
- Integration tests:
  - PostgreSQL con Testcontainers (recomendado).
  - Escenarios: compras concurrentes, sin stock, licencia agotada, cliente deshabilitado no listado.
- Comando base:
  - `./mvnw test`

## CI recommendations
- Ejecutar `./mvnw test` en cada PR.
- Lint/format: agregar solo si el repo lo define (recomendado).
- Build artifacts: `./mvnw -DskipTests package` para generar el JAR.

## Tecnologias
- Java 21, Spring Boot 3.2, Spring Security, Spring Data JPA, PostgreSQL.
- WebClient (reactivo) para webhooks.

## Paso a seguir (recomendado)
1) Ejecutar pruebas: `./mvnw test`.
2) Validar el flujo de compra con licencias multi-activacion (n ventas por licencia).
3) Confirmar integracion n8n y credenciales reales en `application.properties`.
4) Si hay datos en produccion: crear migracion para remover la constraint unica en `orders.license_id`.
