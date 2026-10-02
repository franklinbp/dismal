# Dismal - Blueprint de Estrategia y Administracion

Fecha: 2026-04-28

Este documento define la vision completa antes de continuar desarrollo. La meta es evitar parches aislados y convertir Estrategia y Administracion en un sistema operativo comercial medible.

## 1. Principio rector

Estrategia no debe ser solo una pantalla de alertas. Debe ser un ciclo completo:

1. Detectar senales reales.
2. Priorizar por impacto economico.
3. Recomendar una accion concreta.
4. Ejecutar o asignar esa accion.
5. Medir resultado.
6. Aprender y ajustar metas/campanas.

Administracion no debe ser solo usuarios/configuracion. Debe ser el centro de control de permisos, canales, plantillas, integraciones, auditoria y salud operativa.

## 2. Alcance funcional completo

### Estrategia

Incluye:

- Metas y rentabilidad.
- Inteligencia MIP por producto.
- Campanas comerciales.
- Seguimiento de acciones.
- Priorizacion diaria.
- Cruce con ventas reales.
- Cruce con gastos, cartera e inventario.
- Medicion de resultados.

### Administracion

Incluye:

- Usuarios internos.
- Clientes.
- Roles y permisos.
- Credito y cartera por cliente.
- Importacion/exportacion.
- Integraciones.
- Plantillas de notificacion.
- Outbox y reintentos.
- Auditoria.
- Configuracion operativa.

## 3. Fuente de verdad de datos

La estrategia debe usar estos origenes:

- Ventas reales: `Sale` y `SaleItem`.
- Ventas heredadas/publicas: `Order`, solo como fallback cuando aplique.
- Metas: `SalesTarget`.
- Rentabilidad: `SalesTargetCalculatorService`.
- Gastos: `ExpenseService`.
- Stock/licencias: `LicenseRepository`.
- Interacciones: `ProductInteractionRepository`.
- Campanas: `CampaignRepository`.
- Cartera: `AccountsReceivableRepository`.
- Integraciones: `EventOutboxRepository` y settings.

Regla: frontend no debe inventar reglas criticas de negocio. El backend debe devolver una respuesta estrategica lista para consumir.

## 4. Modelo objetivo de Estrategia

### 4.1 Strategy Overview

Endpoint propuesto/principal:

`GET /api/v1/marketing/intelligence/strategy-overview`

Debe responder:

- metas activas.
- metas cumplidas.
- metas atrasadas.
- productos de prioridad alta.
- alertas comerciales.
- productos pausados.
- ingresos objetivo activos.
- utilidad esperada activa.
- campanas programadas.
- campanas fallidas.
- items prioritarios.

Cada item prioritario debe incluir:

- producto.
- estado MIP.
- prioridad.
- meta unidades.
- unidades vendidas reales.
- avance porcentual.
- ritmo esperado.
- si esta atrasado.
- deadline.
- dias restantes.
- utilidad esperada.
- accion recomendada.
- razon.
- acciones sugeridas.

### 4.2 Strategy Action

Entidad operativa:

`StrategyAction`

Campos recomendados:

- `id`
- `productId`
- `targetId`
- `source`: MIP, META_ATRASADA, CARTERA, STOCK, CAMPANA, MANUAL
- `priority`: ALTA, MEDIA, BAJA
- `status`: PENDIENTE, EN_PROGRESO, HECHA, DESCARTADA
- `title`
- `description`
- `recommendedChannel`: WHATSAPP, EMAIL, LLAMADA, CAMPANA, PRECIO, STOCK
- `assignedTo`
- `dueDate`
- `completedAt`
- `resultNotes`
- `createdAt`
- `updatedAt`

Esto convierte inteligencia en ejecucion real.

Endpoints base:

- `GET /api/v1/marketing/strategy/actions`
- `POST /api/v1/marketing/strategy/actions`
- `PUT /api/v1/marketing/strategy/actions/{id}`
- `PATCH /api/v1/marketing/strategy/actions/{id}/status`
- `DELETE /api/v1/marketing/strategy/actions/{id}` como descarte logico.

### 4.3 Strategy Result

Falta medir impacto:

- ventas generadas despues de accion.
- unidades vendidas.
- utilidad generada.
- pagos recuperados.
- respuesta/click si existe canal.
- campana relacionada.

Sin esto, MIP solo recomienda pero no aprende.

## 5. Modelo objetivo de Administracion

### 5.1 Usuarios y clientes

Mantener:

- CRUD usuarios/clientes.
- roles.
- credito.
- cartera.
- import/export.
- reset password controlado.

Mejorar:

- auditoria de cambios.
- dry-run de importacion.
- reporte de duplicados.
- actualizacion masiva opcional.
- permisos consistentes entre backend y frontend.

### 5.2 Integraciones

Separar la pantalla actual en secciones:

- Canales: n8n, SMTP, CRM, WooCommerce.
- Plantillas: WhatsApp/email por evento.
- Outbox: fallidos, pendientes, reintentos.
- Diagnostico: prueba de envio, salud de credenciales, ultimo error.
- Seguridad: estado de secretos sin exponer valores.

### 5.3 Auditoria

Crear `AuditLog` para acciones administrativas:

- usuario que ejecuta.
- accion.
- entidad afectada.
- antes/despues cuando aplique.
- fecha.
- IP/user agent si existe.

## 6. Permisos objetivo

### ADMIN

Puede todo:

- usuarios internos.
- clientes.
- integraciones.
- estrategia.
- ventas.
- reportes.
- configuracion.

### MANAGER

Puede:

- estrategia.
- metas.
- campanas.
- clientes.
- ventas.
- reportes.
- cartera.

No debe:

- crear/desactivar admins.
- tocar secretos criticos salvo que se decida explicitamente.

### OPERATOR

Puede:

- integraciones operativas.
- reintentar eventos.
- ver outbox.
- pruebas controladas.

No debe:

- cambiar roles.
- cambiar configuracion sensible sin permiso.

## 7. Pantallas objetivo

### Web

Estrategia:

- Resumen ejecutivo.
- Prioridades del dia.
- Metas atrasadas.
- MIP por producto.
- Acciones pendientes.
- Crear campana desde alerta.
- Historial de acciones.
- Pantalla operativa `/admin/marketing/actions` para cambiar estado de acciones.

Administracion:

- Usuarios y clientes.
- Integraciones separadas por panel.
- Auditoria.
- Estado del sistema.

### Desktop

Debe enfocarse en operacion interna:

- vista de prioridades.
- ventas.
- clientes.
- acciones asignadas.
- sync.
- vista `Acciones` dentro del grupo Estrategia para operar pendientes.

No debe quedarse con pantallas en "en desarrollo" para campanas si la navegacion las muestra.

### Android

Debe ser ejecutivo/operativo ligero:

- prioridades del dia.
- metas atrasadas.
- alertas MIP.
- acciones asignadas.
- reportes rapidos.
- resumen de acciones abiertas dentro de Marketing.

## 8. Orden correcto de desarrollo

### Fase 1 - Base de verdad

- MIP usa ventas reales `SaleItem`.
- Backend entrega `strategy-overview`.
- Frontend deja de calcular reglas criticas.
- Corregir encoding critico.

### Fase 2 - Acciones estrategicas

- Crear `StrategyAction`.
- Endpoints CRUD/estado.
- Crear accion desde MIP.
- Crear campana desde accion.
- Asignar responsable.

### Fase 3 - Medicion

- Relacionar accion con campana/venta.
- Calcular resultado posterior.
- Agregar KPIs de impacto.

### Fase 4 - Administracion profesional

- Auditoria.
- Permisos consistentes.
- Integraciones separadas.
- Diagnostico de canales.
- Importacion con dry-run.

### Fase 5 - Apps

- Web completa.
- Desktop sin pantallas simuladas.
- Android con resumen ejecutivo y acciones.

## 9. Criterios de no romper el proyecto

- No eliminar endpoints existentes.
- Agregar endpoints nuevos antes de migrar consumidores.
- Mantener fallback de datos heredados donde exista.
- No cambiar tokens ni credenciales.
- No tocar n8n salvo que sea solicitado.
- Verificar backend y web antes de avanzar de fase.
- Si no hay herramientas locales para compilar, documentar el bloqueo antes de seguir.

## 10. Estado actual despues de la primera intervencion

Ya se inicio parcialmente la Fase 1:

- MIP consulta ventas reales por `SaleItem`.
- Se agrego endpoint `strategy-overview`.
- Web MIP consume resumen estrategico.
- Sidebar permite que OPERATOR vea Integraciones.

Antes de seguir con Fase 2, se debe validar compilacion local o en VPS/CI.
