# Operacion Ecuador, Peru y aplicaciones moviles

## Estado publico verificado el 2026-08-09

- Ecuador responde en `https://www.dismal.net`, muestra 18 productos y el catalogo no registro errores de consola durante la comprobacion.
- Peru responde con `Index of /`; el build existe localmente, pero todavia no esta publicado en la raiz del dominio.
- `https://dismal.vip` responde con la portada de Dismal.
- El navegador de comprobacion bloqueo el acceso directo al health check de `api.dismal.vip`; debe validarse desde el VPS antes de publicar cambios.

## Arquitectura aprobada

- `https://www.dismal.net`: tienda Ecuador, pais `EC`, moneda `USD`.
- `https://www.dismal.net.pe`: tienda Peru, pais `PE`, moneda `PEN`.
- `https://api.dismal.vip`: API central de catalogo, clientes, precios, pedidos, pagos, inventario y licencias.
- `Dismal`: backoffice central y fuente de verdad comercial.
- `DismalCRM`: atencion y seguimiento del cliente.
- `n8n`: orquestacion de notificaciones y tareas posteriores; no es la fuente de verdad de pagos.

Las tiendas se compilan y publican por separado. Comparten API y modelos de negocio, pero nunca mezclan pais, moneda, cuentas bancarias, precio, pedido ni numeracion comercial.

## Flujo autonomo de venta

1. La tienda envia pais, productos y metodo de compra al backend.
2. El backend vuelve a calcular precio, disponibilidad, rol mayorista y cupo de credito.
3. Un cliente con credito aprobado puede confirmar una venta a credito dentro de su cupo.
4. Una transferencia crea un pedido en `PAYMENT_REVIEW`; no entrega licencias hasta la aprobacion administrativa.
5. Una pasarela futura confirma el cobro mediante webhook firmado e idempotente.
6. Solo el backend confirmado crea la venta, descuenta inventario y entrega licencias.
7. El outbox transaccional publica eventos para n8n, Telegram, correo, WhatsApp o CRM sin duplicar la venta.

## Mayoristas

El precio distribuidor se concede solo a una cuenta autenticada con rol `DISTRIBUTOR` aprobado. No se acepta un codigo compartido desde el navegador como prueba de autorizacion. El backend calcula nuevamente el precio antes de registrar la orden.

## n8n y Telegram

Importa `docs/integrations/n8n/dismal-morning-telegram-real-data.json`, configura credenciales dentro de n8n, ejecuta una prueba manual y activa el workflow solo despues de validar los datos. Para ventas se recomiendan eventos como `SALE_CONFIRMED`, `PAYMENT_RECEIVED` y `LICENSES_DELIVERED` desde el outbox.

n8n puede avisar, recordar, escalar y sincronizar. No debe aprobar por si solo una captura de transferencia ni almacenar inventario paralelo.

## Aplicaciones moviles

La app Android de Dismal usa `https://api.dismal.vip/` en release y bloquea trafico HTTP en produccion. Antes de distribuirla necesita firma release, versionado y prueba en un dispositivo fisico.

DismalCRM ya dispone de refresh token nativo protegido en SecureStore. Debe publicarse por un dominio TLS estable, por ejemplo `https://crm.dismal.vip`, con `/api/` y `/socket.io/` dirigidos a sus contenedores. Antes de produccion todavia necesita notificaciones push, adjuntos, actualizacion de dependencias, configuracion EAS y validacion fisica de la renovacion de sesion.

## Condiciones de salida

- DNS y certificados validos para los cuatro hostnames publicos.
- API y web con pruebas de salud desde Internet.
- una orden real de bajo valor por pais, con moneda y cuenta bancaria correctas.
- prueba de cliente final, mayorista aprobado y cliente con credito.
- prueba de idempotencia repitiendo el mismo webhook o aprobacion.
- comprobacion de entrega de licencia, correo, evento outbox y alerta de Telegram.
- copia de seguridad y procedimiento de recuperacion probado.
