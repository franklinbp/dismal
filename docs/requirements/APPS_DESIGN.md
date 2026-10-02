# Dismal Apps - Diseno Funcional (Web, Android, Desktop)

## 1) Diagramas de flujo (ASCII)

### Auth (Login)
```
[Inicio] -> [Pantalla Login] -> [POST /api/v1/auth/authenticate]
  ->(200) [Guardar JWT] -> [Home]
  ->(401/4xx) [Mostrar error]
```

### Registro (Admin/Carga inicial)
```
[Inicio] -> [Pantalla Registro] -> [POST /api/v1/auth/register]
  ->(200) [Guardar JWT] -> [Home]
  ->(400/409) [Mostrar error]
```

### Inventario (Software + Licencias)
```
[Home] -> [Productos]
  -> [Crear Software] -> POST /api/v1/store/product -> [Listado]
  -> [Editar Software] -> PUT /api/v1/store/product/{id} -> [Listado]
  -> [Editar Precio] -> PATCH /api/v1/store/product/{id}/price -> [Listado]
  -> [Crear Licencia] -> POST /api/v1/store/license -> [Detalle Software]
  -> [Desactivar Licencia] -> DELETE /api/v1/store/license/{id}/deactivate -> [Detalle]
```

### Venta (Compra de software)
```
[Home] -> [Catalogo]
  -> [Selecciona Software] -> [Confirmar compra]
  -> [POST /api/v1/sales/purchase (TODO backend)]
  ->(200) [Orden creada] -> [Detalle Orden]
  ->(409) [Sin stock]
```

### Reportes
```
[Home] -> [Reportes]
  -> [Ventas] -> GET /api/v1/reports/sales
  -> [Utilidad] -> GET /api/v1/reports/profit
  -> [Clientes inactivos] -> GET /api/v1/reports/inactive-customers
  -> [Inventario frio] -> GET /api/v1/reports/inventory-alerts
```

### Marketing (Campanas)
```
[Home] -> [Marketing]
  -> [Crear campana] -> POST /api/v1/marketing/campaigns
  -> [Editar campana] -> PUT /api/v1/marketing/campaigns/{id}
  -> [Cancelar] -> DELETE /api/v1/marketing/campaigns/{id}
  -> [Scheduler 15 min] -> envia webhook n8n
```

## 2) Wireframes (ASCII)

### Web (Next.js) - Pantallas principales

Login:
```
+-----------------------------------+
| Dismal | Login                  |
| Email [_______________________]   |
| Pass  [_______________________]   |
| [ Entrar ]                        |
+-----------------------------------+
```

Home/Dashboard:
```
+-------------------------------------------------+
| Dismal Dashboard                              |
| [Productos] [Licencias] [Ventas] [Reportes]     |
| [Clientes]  [Marketing]                         |
|                                                 |
| KPI cards / graficas                            |
+-------------------------------------------------+
```

Productos:
```
+-------------------------------------------------+
| Productos   [Nuevo]                             |
|-------------------------------------------------|
| Nombre | Plataforma | Precio | Stock | Acciones |
| ...                                            |
+-------------------------------------------------+
```

Detalle Software:
```
+----------------------------------------------+
| Software X                                   |
| Precio: $...  Plataforma: ...                |
| [Editar] [Cambiar Precio]                    |
|----------------------------------------------|
| Licencias                                     |
| Key | MaxAct | Used | Status | Acciones       |
| ...                                           |
+----------------------------------------------+
```

Reportes:
```
+----------------------------------------------+
| Reportes                                     |
| [Ventas] [Utilidad] [Clientes] [Inventario]  |
| Grafica / tabla                              |
+----------------------------------------------+
```

### Android (Compose) - Pantallas principales

Login:
```
[Dismal]
Email: [__________]
Pass : [__________]
[ Entrar ]
```

Home:
```
[Catalogo] [Compras]
[Clientes] [Reportes]
[Marketing]
```

Catalogo:
```
Software List
- Producto A  $...
- Producto B  $...
[Ver detalle]
```

Compra:
```
Producto X
Precio $...
[ Comprar ]
```

### Desktop (JavaFX) - Pantallas principales

Login:
```
+-------------------------------+
| Email: [______________]       |
| Pass : [______________]       |
| [ Entrar ]                    |
+-------------------------------+
```

Panel principal:
```
+-------------------------------------------------+
| Menu: Productos | Licencias | Ventas | Reportes |
| Tabla / formulario                              |
+-------------------------------------------------+
```

## 3) Especificacion funcional por pantalla

### 3.1 Login (Web/Android/Desktop)
- Inputs: email, password.
- Validaciones: email requerido, password requerido.
- Accion: POST /api/v1/auth/authenticate.
- Exito: guardar JWT y redirigir a Home.
- Error: mostrar mensaje del backend.

### 3.2 Registro (Admin)
- Inputs: firstname, lastname, email, password.
- Accion: POST /api/v1/auth/register.
- Exito: login automatico con JWT.

### 3.3 Productos (Web/Desktop)
- Lista: GET /api/v1/store/products.
- Crear: POST /api/v1/store/product.
- Editar: PUT /api/v1/store/product/{id}.
- Precio: PATCH /api/v1/store/product/{id}/price.

### 3.4 Licencias (Web/Desktop)
- Crear licencia: POST /api/v1/store/license.
- Campos: softwareId, licenseKey, purchasePrice, maxActivations.
- Desactivar: DELETE /api/v1/store/license/{id}/deactivate.

### 3.5 Catalogo (Android/Web)
- Lista de software (solo lectura): GET /api/v1/store/products.
- Mostrar precio, plataforma, stock calculado (sum activaciones disponibles).

### 3.6 Compra (Android/Web)
- Seleccionar software -> confirmar compra.
- Llamada: `POST /api/v1/sales/purchase` (TODO: exponer endpoint publico de compra).
- Manejo errores: 409 sin stock, 402 credito insuficiente.

### 3.7 Clientes (Web/Desktop)
- Lista: GET /api/v1/customers.
- Resumen: GET /api/v1/customers/summary.
- Actualizar: PUT /api/v1/customers/{id}.
- Desactivar: DELETE /api/v1/customers/{id}.

### 3.8 Reportes (Web/Desktop)
- Ventas: GET /api/v1/reports/sales?startDate=...&endDate=...&period=month.
- Utilidad: GET /api/v1/reports/profit?startDate=...&endDate=....
- Inactivos: GET /api/v1/reports/inactive-customers.
- Inventario frio: GET /api/v1/reports/inventory-alerts.

### 3.9 Marketing (Web/Desktop)
- Crear: POST /api/v1/marketing/campaigns.
- Editar: PUT /api/v1/marketing/campaigns/{id}.
- Cancelar: DELETE /api/v1/marketing/campaigns/{id}.
- Scheduler: envia webhook n8n cada 15 min.

## 4) Acciones pendientes para 100% funcional
- Definir endpoint de compra en controller (si no existe publico).
- Definir endpoint para historial de ordenes (si se necesita en apps).
- Asegurar SDK/cliente API por plataforma.
 - Definir permisos por rol en backend y reflejarlos en UI (bloqueo por rol).
 - Documentar respuesta estandar para compras (ver `docs/requirements/APPS_INTEGRATION.md`).

## Nota
- La UI se implementara en las apps (web/android/desktop).
- El backend puede validarse ahora via Swagger UI y scripts curl.
- En Android (emulador), usar `http://10.0.2.2:8080` como base URL.
