# Storefront: salida a produccion

## Arquitectura aprobada

```text
www.dismal.net      -> hosting compartido, build Ecuador
www.dismal.net.pe   -> hosting compartido, build Peru
api.dismal.vip      -> VPS, Spring Boot, PostgreSQL, pagos y licencias
dismal.vip          -> backoffice y administracion
```

No se instala Node.js, Java ni una base de datos en el hosting compartido.

## 1. Preparar backend

1. Respaldar PostgreSQL.
2. Compilar y desplegar el backend con las migraciones `V18__storefront_orders.sql`, `V19__account_identity.sql`, `V20__storefront_payment_workflow.sql`, `V21__customer_markets.sql` y `V22__sequential_sale_numbers.sql`.
3. Configurar como minimo:

```env
SPRING_PROFILES_ACTIVE=prod
CORS_ALLOWED_ORIGINS=https://dismal.net,https://www.dismal.net,https://dismal.net.pe,https://www.dismal.net.pe
JWT_SECRET=<secreto-base64-de-produccion>
AES_KEY=<clave-de-cifrado-de-produccion>
ACCOUNT_EC_STOREFRONT_URL=https://www.dismal.net
ACCOUNT_PE_STOREFRONT_URL=https://www.dismal.net.pe
SMTP_ENABLED=true
SMTP_HOST=<servidor-smtp>
SMTP_PORT=587
SMTP_USER=<usuario-smtp>
SMTP_PASSWORD=<clave-smtp>
SMTP_TLS=true
SMTP_FROM_EMAIL=<correo-verificado-del-remitente>
SMTP_FROM_NAME=Dismal
```

4. Desde Integraciones, probar SMTP y confirmar que el correo llegue a una cuenta real. No publicar registro ni recuperacion de contrasena con SMTP desactivado.
5. Confirmar HTTPS valido en `api.dismal.vip`.
6. Verificar `GET /actuator/health` y `GET /api/public/products?country=EC`.
7. En el VPS, actualizar el repositorio y ejecutar `./scripts/deploy_easypanel.sh`. El script reconstruye backend y backoffice, los conecta a la red privada de Easypanel e instala las rutas de Traefik.
8. Acceder por `https://dismal.vip/login` y verificar `https://api.dismal.vip/actuator/health`. Los puertos `6001` y `9001` permanecen ligados a `127.0.0.1` exclusivamente para diagnostico desde el VPS.

## 2. Preparar dominios

- `www.dismal.net` es el dominio canonico de Ecuador; `dismal.net` debe redirigirlo permanentemente.
- `www.dismal.net.pe` es el dominio canonico de Peru; `dismal.net.pe` debe redirigirlo permanentemente.
- Activar certificados SSL y redireccion permanente de HTTP a HTTPS desde el panel del hosting.
- Definir un dominio canonico por pais y redirigir la variante `www` para evitar contenido duplicado.

## 3. Compilar y subir

```powershell
cd apps/storefront
npm.cmd ci
npm.cmd run build:all
cd ../..
powershell -ExecutionPolicy Bypass -File scripts/package_storefront.ps1
```

- Subir el contenido de `dist/ecuador` al document root de `www.dismal.net`.
- Subir el contenido de `dist/peru` al document root de `www.dismal.net.pe`.
- No subir `src`, `node_modules`, `.env`, `package.json` ni secretos.
- Los ZIP listos quedan en `apps/storefront/deploy`; cada uno contiene el contenido del document root, incluido `.htaccess`.

## 4. Configurar cobro por transferencia

1. Ingresar al backoffice Dismal como `ADMIN` o `MANAGER`.
2. Abrir `Pedidos web`.
3. En `Cuentas bancarias de la tienda`, registrar al menos:
   - una cuenta `EC` en `USD` para `dismal.net`;
   - una cuenta `PE` en `PEN` para `dismal.net.pe`.
4. Completar banco, titular, numero, tipo de cuenta e identificacion fiscal. Publicar solo cuentas comerciales verificadas.
5. Ejecutar una transferencia real de importe pequeno y comprobar el circuito completo:
   - el cliente crea la orden en `PENDING_PAYMENT`;
   - registra banco de origen, pagador, referencia y monto;
   - la orden cambia a `PAYMENT_REVIEW`;
   - el operador compara esos datos con la banca empresarial;
   - `Aprobar y entregar` crea una sola venta, registra el pago, liquida la cuenta por cobrar y asigna la licencia;
   - `Rechazar` devuelve la orden a `PENDING_PAYMENT` sin consumir inventario.

La referencia bancaria se valida por cuenta receptora y no puede utilizarse en dos ordenes. La aprobacion repetida es idempotente: no duplica venta, pago ni licencia.

## 5. Compra con credito Dismal

El credito no es una forma de pago disponible para cualquier visitante. Para habilitarlo:

1. El cliente debe tener una cuenta activa y correo verificado.
2. Un administrador selecciona Ecuador o Peru y configura `hasCredit`, `creditLimit` y `creditDays` para ese mercado.
3. El cliente inicia sesion y la tienda muestra su cupo disponible.
4. Si el total cabe en el cupo, la orden se confirma como venta `CREDIT`, reserva el cupo, crea la cuenta por cobrar y entrega la licencia.
5. Cuando se registra el cobro posterior, el sistema reduce la deuda y libera el cupo utilizado.

El backend vuelve a calcular precios y cupo. El credito EC se registra en USD y no consume el credito PE en PEN, ni a la inversa. Manipular el navegador no permite obtener credito ni precio distribuidor.

## 6. Precio distribuidor

No se utiliza un codigo compartido, porque podria filtrarse y dar descuentos sin control. El precio distribuidor se concede a una cuenta individual:

1. El cliente crea su cuenta y solicita acceso como distribuidor en su pais.
2. `ADMIN` o `MANAGER` valida el negocio y aprueba la solicitud.
3. La cuenta comercial del pais cambia a `DISTRIBUTOR`.
4. Al iniciar sesion, catalogo y checkout reciben el precio distribuidor calculado por la API.

Un visitante o una cuenta final que solicite el modo distribuidor recibe siempre precio final. Una aprobacion EC no concede automaticamente precio distribuidor en PE.

## 7. Pago automatico futuro

No habilitar entrega automatica hasta completar estas cuatro condiciones:

1. Elegir proveedor autorizado para Ecuador y proveedor autorizado para Peru.
2. Obtener credenciales de comercio para ambiente de pruebas y produccion.
3. Implementar creacion de sesion de pago y webhook con validacion criptografica.
4. Probar pago aprobado, rechazado, duplicado, monto incorrecto, reembolso y webhook repetido.

El webhook debe llamar al servicio de confirmacion existente. El navegador nunca puede marcar una orden como pagada. Mientras no exista un proveedor con API bancaria o pasarela y firma de webhook, la verificacion bancaria es manual desde `Pedidos web`; no debe simularse como automatica.

## 8. Datos comerciales obligatorios

Antes de publicar, reemplazar o configurar:

- WhatsApp real de Ecuador y Peru.
- Razon social, RUC y datos fiscales aplicables.
- Terminos de compra, privacidad, reembolsos y garantia.
- Correos de soporte y entrega.
- Logos e imagenes reales de los productos vendidos.
- Pasarelas y metodos de pago realmente disponibles.

## 9. Prueba de aceptacion

- El dominio EC muestra USD y crea orden EC.
- El dominio PE muestra PEN y crea orden PE.
- Un visitante no ve precios de distribuidor.
- Una cuenta final no obtiene precio de distribuidor.
- Una cuenta distribuidora aprobada ve y compra con su tarifa solo en el pais aprobado.
- Un email ya registrado exige iniciar sesion.
- Un registro nuevo recibe el enlace correcto para su pais y puede verificar el correo una sola vez.
- Recuperar contrasena no revela si el correo existe y el enlace expira despues de 30 minutos.
- Cambiar o restablecer la contrasena invalida las sesiones anteriores.
- Una cuenta sin correo verificado no puede ver las claves de sus licencias.
- Una solicitud de distribuidor queda pendiente por pais hasta que ADMIN o MANAGER la apruebe.
- Aprobar una solicitud cambia la cuenta a distribuidor y conserva revisor, fecha y observaciones.
- Una orden pendiente no consume inventario.
- Una transferencia registrada queda `PAYMENT_REVIEW` y tampoco consume inventario.
- Rechazar una transferencia no crea venta, pago ni licencia.
- Un pago confirmado una sola vez crea venta, pago y licencia.
- Repetir la aprobacion del mismo pago no duplica registros.
- Una cuenta verificada con credito suficiente compra sin transferencia y genera cuenta por cobrar.
- Una cuenta sin credito o sin cupo no puede elegir credito.
- El cliente autenticado ve la orden y su licencia.
- El sitio funciona a 360 px y 1440 px sin desbordes.

## 10. Reversion

Conservar el build anterior de cada dominio. Si la validacion posterior falla, restaurar esos archivos estaticos; si el problema esta en backend, restaurar la version anterior de la aplicacion sin eliminar la migracion ni las ordenes ya creadas.
