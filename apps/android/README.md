# Dismal Android App

Este es el cliente móvil administrativo para el sistema Dismal. Permite gestionar clientes, productos, ventas, cobros y reportes directamente desde dispositivos Android, con soporte para operación offline y sincronización manual.

## Estado del Proyecto

La aplicación se encuentra en una fase avanzada de implementación, siguiendo una arquitectura limpia (Clean Architecture) con módulos separados para `data`, `domain`, `core` y `features`.

### Funcionalidades Implementadas

*   **Autenticación**: Login seguro con JWT y almacenamiento cifrado del token.
*   **Dashboard**: Resumen operativo con ventas del día/mes, utilidad, cartera vencida y metas.
*   **Clientes**: Gestión completa de clientes (listar, buscar, crear).
*   **Catálogo**: Visualización de productos y software con precios comerciales.
*   **Ventas**: Generación de ventas (Contado/Crédito), confirmación y registro de pagos.
*   **Cobros**: Seguimiento de cuentas por cobrar y registro de abonos.
*   **Notificaciones**: Envío de licencias y listas de precios por Email y WhatsApp (vía DismalCRM).
*   **Reportes**: Acceso a reportes comerciales y operativos.
*   **Offline-first**: Base de datos local (Room) que permite trabajar sin conexión y sincronizar cambios posteriormente.

## Configuración para Desarrollo e Implementación

Para que la app se conecte correctamente al backend, es necesario configurar el archivo `local.properties` en la raíz del proyecto Android.

### 1. Archivo local.properties

Crea o edita `Dismal/apps/android/local.properties`:

```properties
DISMAL_API_URL=https://api.dismal.vip/
DISMAL_LOCAL_URL=http://10.0.2.2:8080/
```

*   **DISMAL_API_URL**: URL del VPS o servidor de producción. Debe terminar en `/`.
*   **DISMAL_LOCAL_URL**: URL para el emulador (por defecto `10.0.2.2:8080`).

### 2. Implementación en Teléfono Real

Consulta el manual detallado:
[Manual de Implementación en Teléfono](../../docs/APP_MOVIL_IMPLEMENTACION_TELEFONO.md)

Pasos rápidos:
1. Activar **Opciones de Desarrollador** y **Depuración USB** en el teléfono.
2. Conectar el teléfono a la PC.
3. En Android Studio, seleccionar el dispositivo y presionar **Run**.
4. Para distribución, generar un **APK firmado** (Build > Generate Signed Bundle / APK).

## Arquitectura

*   **UI**: Jetpack Compose con Hilt para Inyección de Dependencias.
*   **Datos**: Retrofit para API REST y Room para persistencia local.
*   **Sincronización**: WorkManager para tareas de fondo.
*   **Seguridad**: `EncryptedSharedPreferences` para datos sensibles.

## Sincronización Offline

La app utiliza una política de sincronización manual:
1. Al presionar **Sync**, se envían los cambios locales pendientes (`pushOutbox`).
2. Luego se descargan los datos actualizados del servidor (`pull`).
3. Los conflictos deben resolverse aceptando la versión del servidor.

---
Para más detalles técnicos, consulta el [Manual Técnico Android](../../docs/APP_ANDROID_MANUAL_TECNICO.md).
