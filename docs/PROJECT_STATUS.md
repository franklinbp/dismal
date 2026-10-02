# Estado del Proyecto DISMAL - Reporte de Ingeniería

**Fecha:** 16 de Enero, 2026  
**Rol:** Arquitecto de Software / DevOps  
**Estado Global:** 🟢 Backend Estable | 🟡 Desktop en Refactorización Final | ⚪ Android en Inicio

---

## 1. Hitos Logrados (Últimas 24h)

### A. Backend (Seguridad y Ventas)
- **RBAC (Role-Based Access Control):** Se implementó seguridad por métodos (`@PreAuthorize`). Ahora solo usuarios con rol `ADMIN` o `MANAGER` pueden gestionar inventario y usuarios.
- **Módulo de Compra Pública:** Se creó `PublicSalesController` para permitir que los clientes finales realicen compras de licencias directamente.
- **Integración n8n:** Verificación de flujos de notificación vía webhooks para órdenes pagadas.

### B. Aplicación de Escritorio (Arquitectura Local-First)
- **Migración a Spring Boot:** La app desktop ya no es solo una UI; ahora tiene un backend embebido que gestiona su propia base de datos.
- **Persistencia Local (H2):** Se configuró una base de datos local en `~/.dismal/data/local_db`. La app es funcional sin internet una vez sincronizada.
- **SyncService:** Motor de sincronización capaz de hacer PULL de productos, licencias y metas desde la nube al PC local.

### C. UX/UI (Diseño Corporativo)
- **Tema "Clean Light":** Reemplazo del tema oscuro por uno blanco/gris de alto contraste para máxima legibilidad.
- **Patrón "Sliding Drawer":** Implementación de paneles laterales deslizables para detalles en los módulos de:
    - Inventario (Productos y Claves).
    - Administración (Usuarios, Planeación y Marketing).
- **Navegación:** Implementación de un Menú Acordeón colapsable.

---

## 2. Arquitectura de Sincronización (Roadmap para Android)

La lógica aplicada en Desktop debe ser replicada en Android:
1. **PULL Inicial:** Descarga de catálogo completo.
2. **Operación Local:** Creación de ventas y edición de perfiles en base de datos local (SQLite/Room).
3. **PUSH Eventual:** Al detectar conexión, enviar las transacciones pendientes a la API del VPS.
4. **Conflicto:** El servidor tiene la autoridad final en stock de licencias.

---

## 3. Módulo MIP (Marketing Inteligente por Producto) - COMPLETADO (Backend)

Hemos implementado la lógica completa para que el sistema clasifique automáticamente los productos:
- **Estados:** RENTABLE, BAJA_TRACCION, ALERTA_COMERCIAL, PAUSADO.
- **Motor de Reglas:** `MIPEngineService` analiza ventas e interacciones.
- **Diagnóstico:** Identifica si el problema es de Visibilidad, Confianza, Precio o Complejidad.
- **Acciones:** Propone estrategias automáticas (Testimonios, Guías, Bundles).
- **Tracking:** Endpoint `/api/v1/marketing/intelligence/interactions` disponible para registrar el interés del usuario.

---

## 4. Backlog de Pendientes (Próximos Pasos)

1. **Backend:**
    - [ ] Crear endpoint de "Sincronización Delta" (recibir solo cambios desde fecha X).
2. **Desktop:**
    - [ ] Integrar vista de MIP (Marketing Inteligente) para visualizar el análisis de productos.
    - [ ] Implementar gráficos (Charts) en el Dashboard para igualar la web.
    - [ ] Refinar el "Carrito de Compras" en el POS para manejar múltiples cantidades.
3. **Android:**
    - [ ] Configurar Retrofit con el esquema de DTOs definido en el reporte de Android.
    - [ ] Implementar la base de datos Room siguiendo el esquema del VPS.
    - [ ] Llamar a `/interactions` cuando el usuario vea un producto.

---

## 5. Instrucciones de Ejecución

### Backend
```bash
./mvnw spring-boot:run
```

### Desktop
```bash
./mvnw -f apps/desktop/pom.xml spring-boot:run
```

### Web
```bash
cd apps/web && npm run dev
```
