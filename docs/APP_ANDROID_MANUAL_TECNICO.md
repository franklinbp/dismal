# Manual Tecnico - App Android Dismal

## 1. Objetivo

La app Android de Dismal es el cliente movil administrativo del sistema. Su objetivo es permitir que el equipo autorizado pueda consultar informacion operativa, gestionar clientes, generar ventas, revisar cobros, enviar listas de precios y trabajar con datos locales cuando la conexion no esta disponible.

La app no reemplaza al backend. Consume la API de Dismal y mantiene una base local para consulta, operacion movil y sincronizacion.

## 2. Ubicacion del Proyecto

```text
Dismal/apps/android
```

Estructura principal:

```text
apps/android/
  app/                 Aplicacion Android principal, navegacion, pantallas, DI y WorkManager
  core/                Utilidades compartidas de UI
  data/                Retrofit, Room, repositorios, autenticacion y sincronizacion
  domain/              Modelos de negocio usados por la app
  features/products/   Modulo funcional de productos
  features/customers/  Modulo funcional de clientes
```

## 3. Tecnologia Utilizada

- Kotlin
- Jetpack Compose para la interfaz
- Hilt para inyeccion de dependencias
- Retrofit + OkHttp para consumir la API
- Moshi para JSON
- Room para base de datos local
- WorkManager para sincronizacion en segundo plano
- AndroidX Security Crypto para almacenamiento seguro
- Coil para carga de imagenes

Configuracion base:

```text
applicationId: com.dismal.app
minSdk: 24
targetSdk: 36
compileSdk: 36
versionName: 1.0
versionCode: 1
```

## 4. Configuracion de API

La URL del backend se define en:

```text
apps/android/local.properties
```

Existe un ejemplo en:

```text
apps/android/local.properties.example
```

Formato:

```properties
DISMAL_API_URL=https://tu-dominio-o-ip/
DISMAL_LOCAL_URL=http://10.0.2.2:8080/
```

Uso:

- `DISMAL_API_URL`: URL de produccion o VPS.
- `DISMAL_LOCAL_URL`: URL para desarrollo local.
- En `debug`, si existe `DISMAL_LOCAL_URL`, usa esa URL.
- En `release`, usa `DISMAL_API_URL`.

La URL siempre debe terminar con `/`.

## 5. Flujo de Autenticacion

La app inicia en `BootScreen`.

Flujo:

```text
BootScreen
  -> Si hay token valido: Dashboard
  -> Si no hay token: Login
```

Archivos principales:

```text
app/src/main/java/com/dismal/app/navigation/AppNavigation.kt
app/src/main/java/com/dismal/app/ui/session/SessionViewModel.kt
app/src/main/java/com/dismal/app/ui/login/LoginScreen.kt
app/src/main/java/com/dismal/app/ui/login/LoginViewModel.kt
data/src/main/java/com/dismal/app/data/auth/AuthApi.kt
data/src/main/java/com/dismal/app/data/auth/TokenStore.kt
```

Endpoint usado:

```http
POST api/v1/auth/authenticate
```

El token se guarda en `TokenStore`. Las peticiones HTTP pasan por `AuthInterceptor`, que agrega el token al request.

## 6. Roles y Acceso

El dashboard administrativo movil esta pensado para roles:

```text
ADMIN
MANAGER
```

La logica esta en:

```text
app/src/main/java/com/dismal/app/ui/dashboard/DashboardViewModel.kt
```

Si el usuario no es `ADMIN` o `MANAGER`, la app limita el menu visible a:

```text
Perfil
Salir
```

Esto evita que usuarios sin permiso administrativo vean ventas, reportes, cobros o configuracion movil.

## 7. Pantallas Implementadas

La navegacion interna del dashboard esta en:

```text
app/src/main/java/com/dismal/app/ui/dashboard/DashboardScreen.kt
```

Rutas principales:

```text
home          Dashboard principal
customers     Clientes
priceList     Lista de precios
sales         Ventas
reports       Reportes
marketing     Estrategia / inteligencia comercial
billing       Facturas
collections   Cobros / cuentas por cobrar
profile       Perfil
logout        Salir
```

En pantallas anchas usa `NavigationRail`. En pantallas moviles usa barra inferior con accesos principales.

## 8. Funcionalidades Principales

### 8.1 Dashboard

Muestra resumen operativo:

- Ventas del dia
- Monto vendido del dia
- Ventas del mes
- Monto vendido del mes
- Utilidad esperada
- Cartera vencida
- Metas prioritarias

APIs:

```http
GET api/v1/dashboard/admin/summary
GET api/v1/dashboard/admin/top-targets
```

Repositorio:

```text
data/src/main/java/com/dismal/app/data/repository/DashboardRepository.kt
```

### 8.2 Clientes

Permite consultar clientes y crear clientes desde el movil.

APIs:

```http
GET  api/v1/customers
GET  api/v1/admin/users?type=CLIENT
POST api/v1/admin/users
POST api/v1/customers/sync
```

Archivos:

```text
app/src/main/java/com/dismal/app/ui/customers/
data/src/main/java/com/dismal/app/data/repository/CustomerRepository.kt
data/src/main/java/com/dismal/app/data/network/CustomerApi.kt
```

### 8.3 Ventas

Permite crear ventas desde el movil, confirmar ventas y registrar pagos.

APIs:

```http
GET  api/v1/sales
POST api/v1/sales
POST api/v1/sales/{id}/confirm
POST api/v1/payments
```

Archivos:

```text
app/src/main/java/com/dismal/app/ui/sales/
data/src/main/java/com/dismal/app/data/repository/SaleRepository.kt
data/src/main/java/com/dismal/app/data/network/SalesApi.kt
data/src/main/java/com/dismal/app/data/network/PaymentsApi.kt
```

Uso esperado:

```text
Seleccionar cliente
Seleccionar producto/licencia
Definir tipo de venta
Crear venta
Confirmar venta
Registrar pago si aplica
Sincronizar con backend
```

### 8.4 Cobros / Cuentas por Cobrar

Permite revisar cuentas abiertas por cliente y apoyar seguimiento de cartera.

API:

```http
GET api/v1/accounts-receivable/client/{clientId}/open
```

Archivos:

```text
app/src/main/java/com/dismal/app/ui/collections/
data/src/main/java/com/dismal/app/data/network/AccountsReceivableApi.kt
data/src/main/java/com/dismal/app/data/repository/AccountsReceivableRepository.kt
domain/src/main/java/com/dismal/app/domain/models/AccountsReceivable.kt
```

### 8.5 Lista de Precios

Permite enviar lista de precios desde el movil usando la API del backend.

API:

```http
POST api/v1/reports/price-list/send
```

Archivos:

```text
app/src/main/java/com/dismal/app/ui/pricelist/
data/src/main/java/com/dismal/app/data/repository/PriceListRepository.kt
data/src/main/java/com/dismal/app/data/network/ReportsApi.kt
domain/src/main/java/com/dismal/app/domain/models/PriceListSendRequest.kt
domain/src/main/java/com/dismal/app/domain/models/PriceListSendResult.kt
```

### 8.6 Inventario y Licencias

Consulta licencias disponibles y datos de inventario desde backend.

Archivos:

```text
data/src/main/java/com/dismal/app/data/network/InventoryApi.kt
data/src/main/java/com/dismal/app/data/repository/InventoryRepository.kt
data/src/main/java/com/dismal/app/data/db/LicenseEntity.kt
```

### 8.7 Facturas

Modulo movil para listar y crear facturas desde la API de Dismal.

Archivos:

```text
app/src/main/java/com/dismal/app/ui/billing/
data/src/main/java/com/dismal/app/data/network/InvoicesApi.kt
data/src/main/java/com/dismal/app/data/repository/InvoiceRepository.kt
domain/src/main/java/com/dismal/app/domain/models/Invoice.kt
domain/src/main/java/com/dismal/app/domain/models/CreateInvoiceRequest.kt
```

### 8.8 Marketing / Estrategia

Consulta analisis comercial y acciones sugeridas.

Archivos:

```text
app/src/main/java/com/dismal/app/ui/marketing/
data/src/main/java/com/dismal/app/data/network/MarketingIntelligenceApi.kt
data/src/main/java/com/dismal/app/data/repository/MarketingIntelligenceRepository.kt
domain/src/main/java/com/dismal/app/domain/models/MarketingAnalysis.kt
domain/src/main/java/com/dismal/app/domain/models/StrategyAction.kt
```

## 9. Base de Datos Local

La app usa Room con la base:

```text
dismal_database
```

Archivo:

```text
data/src/main/java/com/dismal/app/data/db/AppDatabase.kt
```

Version actual:

```text
12
```

Tablas:

```text
products
customers
sales
licenses
sales_targets
sync_outbox
```

DAOs:

```text
ProductDao
CustomerDao
SaleDao
LicenseDao
SalesTargetDao
SyncOutboxDao
```

En `debug`, la app puede aplicar `fallbackToDestructiveMigration()` y borrar la base local cuando cambia la version de desarrollo. En `release`, usa migraciones formales `MIGRATION_1_2` hasta `MIGRATION_11_12`.

## 10. Sincronizacion Offline

La app implementa una politica offline-first.

Reglas:

- La base local es la fuente de trabajo movil.
- Las operaciones locales pendientes se guardan en `sync_outbox`.
- La sincronizacion primero envia pendientes y luego descarga datos del servidor.
- Si no hay token, no sincroniza.
- Si esta activo el modo offline, el `SyncWorker` termina sin enviar datos.

Archivo principal:

```text
data/src/main/java/com/dismal/app/data/repository/SyncRepository.kt
```

Worker:

```text
app/src/main/java/com/dismal/app/sync/SyncWorker.kt
```

Orden de sincronizacion:

```text
1. pushOutbox()
2. refreshProducts()
3. refreshLicenses()
4. refreshCustomers()
5. refreshTargets()
6. refreshSales()
7. actualizar estado de ultima sincronizacion
```

Tipos de outbox soportados:

```text
SALE
CUSTOMER
```

Estados:

```text
PENDING
SENT
ERROR
```

## 11. Seguridad

La app usa:

- `INTERNET` como unico permiso declarado.
- `TokenStore` para token, rol, email, modo offline y estado de sincronizacion.
- `AuthInterceptor` para agregar autenticacion a Retrofit.
- `network_security_config` para control de trafico de red.
- ProGuard/R8 en `release`.

Archivo:

```text
app/src/main/AndroidManifest.xml
app/src/main/res/xml/network_security_config.xml
app/proguard-rules.pro
```

## 12. Construccion

Desde:

```bash
cd Dismal/apps/android
```

Crear `local.properties`:

```properties
DISMAL_API_URL=https://api.tu-dominio.com/
DISMAL_LOCAL_URL=http://10.0.2.2:8080/
```

Compilar debug:

```bash
./gradlew assembleDebug
```

Compilar release:

```bash
./gradlew assembleRelease
```

APK debug:

```text
app/build/outputs/apk/debug/app-debug.apk
```

APK release:

```text
app/build/outputs/apk/release/app-release.apk
```

Para instalar en telefono conectado por USB:

```bash
./gradlew installDebug
```

## 13. Implementacion en Telefono

Requisitos:

- Android 7.0 o superior.
- Conexion al backend de Dismal.
- Usuario con rol `ADMIN` o `MANAGER` para operar los modulos administrativos.

Pasos:

```text
1. Configurar DISMAL_API_URL apuntando al VPS o dominio.
2. Compilar APK.
3. Instalar APK en el telefono.
4. Abrir la app.
5. Iniciar sesion con usuario valido.
6. Verificar Dashboard.
7. Probar sincronizacion.
8. Probar crear cliente.
9. Probar generar venta de bajo impacto o en ambiente de pruebas.
10. Confirmar que la venta aparece en el sistema web.
```

## 14. Relacion con Backend Dismal

La app depende de endpoints existentes en el backend Spring Boot de Dismal. Las operaciones criticas no se resuelven localmente de forma definitiva; se sincronizan con el backend.

Contratos principales:

```text
Auth            api/v1/auth/authenticate
Usuario         api/v1/users/me
Dashboard       api/v1/dashboard/admin/*
Clientes        api/v1/customers, api/v1/admin/users
Ventas          api/v1/sales
Pagos           api/v1/payments
Inventario      api/v1/inventory / licencias
Lista precios   api/v1/reports/price-list/send
Cobros          api/v1/accounts-receivable/*
Facturas        api/v1/invoices
Marketing       api/v1/marketing-intelligence/*
```

## 15. Riesgos Tecnicos y Recomendaciones

1. El README de Android esta desactualizado frente a la funcionalidad real. Este manual debe ser la referencia principal.
2. La app depende de que `DISMAL_API_URL` este bien configurado y termine en `/`.
3. En produccion no se debe usar una URL local tipo `10.0.2.2`.
4. Las pruebas de ventas reales deben hacerse con cuidado porque pueden consumir licencias y disparar notificaciones.
5. Si se agregan nuevas tablas Room, siempre aumentar `version` y crear migracion.
6. Si se cambia un DTO en backend, revisar el modelo equivalente en `domain` y `data/network/dto`.
7. Para publicar en Play Store o distribuir formalmente, falta definir firma release, versionado y politica de actualizaciones.

## 16. Checklist de Mantenimiento

Antes de subir cambios:

```text
Revisar endpoints modificados en backend.
Actualizar DTO/modelos Android.
Ejecutar pruebas unitarias.
Compilar assembleDebug.
Probar login.
Probar dashboard.
Probar sincronizacion.
Probar una venta controlada en ambiente seguro.
Verificar que no se consuman licencias reales sin autorizacion.
```

Comandos utiles:

```bash
./gradlew test
./gradlew connectedAndroidTest
./gradlew assembleDebug
./gradlew assembleRelease
```

