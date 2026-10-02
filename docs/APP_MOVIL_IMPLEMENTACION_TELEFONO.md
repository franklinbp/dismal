# Manual de implementacion de la app movil Dismal

## Objetivo

Dejar la app Android de Dismal lista para instalar y operar en un telefono real, conectada al backend de Dismal en el VPS.

La app se encuentra en:

```text
Dismal/apps/android
```

## Estado funcional revisado

La app movil tiene implementado:

- Login con el backend de Dismal.
- Dashboard administrativo.
- Clientes.
- Catalogo/productos.
- Generacion de ventas.
- Confirmacion de venta.
- Envio de licencia por email y WhatsApp usando el backend.
- Registro de pago completo para ventas de contado.
- Cobros de cuentas por cobrar reales mediante `/api/v1/ar/client/{clientId}` y `/api/v1/payments`.
- Lista de precios por email y WhatsApp.
- Reportes.
- Invoices/facturacion interna.
- Marketing.
- Perfil, logout y PIN offline.
- Base local Room y sincronizacion manual.

## Requisitos

En la computadora de desarrollo:

- Android Studio instalado.
- JDK configurado. Recomendado: JDK 17.
- Git.
- Acceso al backend Dismal en el VPS.

En el VPS:

- Backend Dismal corriendo.
- API accesible desde internet o desde la red del telefono.
- Usuario con rol `ADMIN` o `MANAGER`.
- Integraciones de email y WhatsApp funcionando en el backend.
- Si se usa WhatsApp, DismalCRM debe estar conectado y el backend Dismal debe tener las variables `DISMAL_CRM_*` correctas.

## Configuracion de URL

Crear este archivo:

```text
Dismal/apps/android/local.properties
```

Ejemplo para VPS:

```properties
DISMAL_API_URL=https://TU-DOMINIO-O-IP/
DISMAL_LOCAL_URL=http://10.0.2.2:8080/
```

Reglas importantes:

- La URL debe terminar con `/`.
- Para telefono real no usar `localhost`.
- Para emulador Android se puede usar `http://10.0.2.2:8080/`.
- Para produccion se recomienda HTTPS con dominio real.

Si todavia usas IP y HTTP, Android lo permite porque la app tiene `network_security_config` con cleartext habilitado. Aun asi, para produccion comercial se recomienda HTTPS.

## Compilar APK para prueba

Desde Android Studio:

1. Abrir `Dismal/apps/android`.
2. Esperar sincronizacion Gradle.
3. Confirmar que `local.properties` tenga `DISMAL_API_URL`.
4. Ir a `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
5. El APK queda normalmente en:

```text
Dismal/apps/android/app/build/outputs/apk/debug/app-debug.apk
```

Desde terminal:

```bash
cd Dismal/apps/android
./gradlew :app:assembleDebug
```

En Windows:

```powershell
cd Dismal\apps\android
.\gradlew.bat :app:assembleDebug
```

## Instalar en telefono Android

Opcion A: Android Studio

1. Activar opciones de desarrollador en el telefono.
2. Activar depuracion USB.
3. Conectar telefono por cable.
4. Seleccionar el dispositivo en Android Studio.
5. Presionar Run.

Opcion B: instalar APK manual

1. Copiar `app-debug.apk` al telefono.
2. Abrir el APK desde el telefono.
3. Permitir instalacion desde origen desconocido.
4. Instalar.

Opcion C: ADB

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Generar APK firmado para uso real

En Android Studio:

1. `Build > Generate Signed Bundle / APK`.
2. Elegir `APK` o `Android App Bundle`.
3. Crear o seleccionar keystore.
4. Usar build type `release`.
5. Generar.

Guardar de forma segura:

- Archivo `.jks`.
- Alias.
- Password del keystore.
- Password de la key.

Sin ese keystore no se podra actualizar la misma app instalada.

## Checklist antes de probar en telefono

En el VPS:

```bash
cd /root/Dismal
docker compose ps
docker compose logs backend --since=5m
```

Verificar:

- Backend sin errores.
- Base de datos conectada.
- DismalCRM configurado si se enviara WhatsApp.
- Email configurado si se enviara correo.

En Android:

- Instalar app.
- Abrir app.
- Login con usuario `ADMIN` o `MANAGER`.
- Entrar al dashboard.
- Actualizar clientes/productos.

## Prueba controlada sin basura

Para no generar informacion basura en produccion, usar un cliente real de prueba interno:

- Nombre: Cliente Prueba Dismal.
- Email controlado.
- WhatsApp controlado.
- Producto/licencia de bajo riesgo o licencia preparada para prueba.

Flujo recomendado:

1. Crear o confirmar que existe el cliente de prueba.
2. Crear venta desde la app movil.
3. Seleccionar producto.
4. Tipo:
   - `CASH` si se cobrara al momento.
   - `CREDIT` si debe quedar en cuentas por cobrar.
5. Para contado dejar activo `Registrar pago completo`.
6. Elegir metodo:
   - `CASH`
   - `CARD`
   - `TRANSFER`
7. Activar o desactivar email/WhatsApp segun lo que se quiera probar.
8. Presionar `Generar venta`.

Resultado esperado:

- La venta queda confirmada.
- Si es contado y se registro pago completo, la venta queda pagada.
- Si se activo email/WhatsApp, el envio sale desde el backend.
- La licencia se entrega segun la logica actual de Dismal.

## Prueba de cobros

1. Crear una venta a credito desde web o app.
2. Ir a `Cobros` en la app.
3. Seleccionar cliente con saldo.
4. Presionar `Registrar cobro`.
5. Ingresar monto.
6. Elegir metodo: `CASH`, `CARD` o `TRANSFER`.
7. Agregar referencia si aplica.
8. Guardar.

Resultado esperado:

- La app consulta cuentas por cobrar del cliente.
- Registra el pago contra la venta mas antigua con saldo.
- Si el pago cubre el total, la venta queda pagada.
- El saldo del cliente se actualiza al refrescar.

## Lista de precios desde movil

1. Ir a `Lista de precios`.
2. Ingresar nombre del cliente.
3. Para email:
   - Completar email.
   - Elegir asunto.
   - Marcar PDF/Excel si aplica.
4. Para WhatsApp:
   - Completar numero.
5. Seleccionar columnas de precios.
6. Enviar.

Resultado esperado:

- Email usa `/api/v1/reports/price-list/send`.
- WhatsApp usa el mismo backend y las integraciones configuradas.

## Variables relacionadas en backend

Para que WhatsApp funcione desde ventas/listas:

```properties
DISMAL_CRM_ENABLED=true
DISMAL_CRM_BASE_URL=http://62.171.142.234:8080
DISMAL_CRM_API_TOKEN=...
DISMAL_CRM_WHATSAPP_ENABLED=true
DISMAL_CRM_WHATSAPP_ID=34
```

El `WHATSAPP_ID` debe existir y estar conectado en DismalCRM.

## Comandos utiles en VPS

Logs backend Dismal:

```bash
cd /root/Dismal
docker compose logs backend --since=10m
```

Verificar contenedores:

```bash
docker compose ps
```

Rebuild backend/web:

```bash
docker compose up -d --build backend web
```

## Problemas comunes

### No inicia sesion

- Revisar `DISMAL_API_URL`.
- Confirmar que termina en `/`.
- Probar URL desde navegador del telefono.
- Revisar credenciales y rol.

### Funciona en emulador pero no en telefono

- El telefono no puede usar `localhost`.
- Usar dominio/IP publica del VPS.
- Confirmar firewall y puertos.

### WhatsApp no llega

- Revisar logs de Dismal.
- Revisar logs de DismalCRM.
- Confirmar `DISMAL_CRM_WHATSAPP_ID`.
- Confirmar que la sesion WhatsApp este `CONNECTED`.

### Email no llega

- Revisar integraciones de correo.
- Revisar logs backend.
- Confirmar que el email destino sea valido.

### Cobro no se registra

- El cliente debe tener cuenta por cobrar abierta.
- El usuario debe ser `ADMIN` o `MANAGER`.
- El metodo debe ser `CASH`, `CARD` o `TRANSFER`.

## Recomendacion para produccion

Para uso real en clientes o equipo comercial:

1. Usar dominio HTTPS.
2. Generar APK firmado.
3. Crear versionCode/versionName por cada entrega.
4. No distribuir `debug.apk` fuera de pruebas internas.
5. Mantener keystore respaldado.
6. Probar primero con un cliente interno controlado.

