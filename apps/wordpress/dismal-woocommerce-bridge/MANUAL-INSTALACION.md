# Manual de Instalacion

## Objetivo

Dejar WooCommerce listo para:

- importar clientes existentes desde `Dismal`
- autenticar clientes con su cuenta de `Dismal`
- sincronizar ordenes pagadas hacia `Dismal`
- permitir compras con credito aprobado en `Dismal`

## Requisitos

- WordPress con WooCommerce activo
- plugin `dismal-woocommerce-bridge`
- backend `Dismal` desplegado
- valor configurado en backend:
  - `integrations.woocommerce.api-key`

## Paso 1. Subir plugin

1. Copia la carpeta `dismal-woocommerce-bridge` a:
   - `wp-content/plugins/`
2. Activa el plugin en el administrador de WordPress.

## Paso 2. Configurar backend

En el backend `Dismal`, define una clave de integracion para WooCommerce:

- propiedad:
  - `integrations.woocommerce.api-key`

La misma clave debe colocarse luego en WooCommerce.

## Paso 3. Configurar WooCommerce

En WordPress ve a:

- `WooCommerce > Dismal Bridge`

Completa:

- `API Base URL`
  - ejemplo: `https://api.tudominio.com`
- `Integration Key`
  - el mismo valor configurado en `integrations.woocommerce.api-key`
- `Paid Order Statuses`
  - normalmente `processing` y `completed`
- `Default Customer Type`
- `Distributor WordPress Roles`
- `Payment Method Map`

Guarda los cambios.

Presiona:

- `Probar conexion con Dismal`

Resultado esperado:

- `Conexion correcta con Dismal. Clientes disponibles: X.`

Luego ve a:

- `WooCommerce > Ajustes > Pagos`

Activa `Credito Dismal` solo cuando los clientes ya hayan sido importados desde `Dismal`.

## Paso 4. Mapear productos

En cada producto WooCommerce:

1. abre el producto
2. en datos generales completa:
   - `Dismal Software ID`

Ese valor debe ser el UUID real del software en `Dismal`.

## Paso 5. Importar clientes existentes

En la misma pantalla:

- `WooCommerce > Dismal Bridge`

usa:

- `Importar clientes ahora`

Que hace:

- lee clientes paginados desde `Dismal`
- crea usuarios WooCommerce si no existen
- enlaza usuarios existentes por email
- guarda `dismal_user_id` en meta de usuario
- no modifica contraseñas
- guarda credito disponible para decidir si el checkout puede mostrar `Credito Dismal`

## Paso 6. Validar acceso de clientes

Pide a un cliente existente que entre con:

- `email`
- `password de Dismal`

Resultado esperado:

- el plugin autentica contra `Dismal`
- WooCommerce crea o reutiliza la cuenta local
- el cliente puede entrar al sitio

## Paso 7. Validar orden de prueba

Haz una compra de prueba y revisa:

1. la orden WooCommerce entra en `processing` o `completed`
2. el plugin envia la orden a `Dismal`
3. se crea la venta en `Dismal`
4. se confirma y cobra
5. la orden queda con:
   - `_dismal_sale_id`
   - `_dismal_sync_status`

Nota:

- Si la orden ya tiene `_dismal_sale_id` y estado `synced` o `duplicated`, el plugin no la reenvia automaticamente.
- El boton `Retry Sync` en la orden fuerza un reintento manual.

## Paso 8. Validar compra a credito

Con un cliente importado que tenga credito disponible:

1. inicia sesion en WordPress
2. agrega un producto con `Dismal Software ID`
3. selecciona `Credito Dismal`
4. finaliza la compra
5. valida en `Dismal`:
   - venta tipo `CREDIT`
   - licencia consumida y enviada
   - cuenta por cobrar abierta
   - credito usado incrementado

## Notas operativas

- La fuente maestra del cliente es `Dismal`
- WooCommerce funciona como storefront y cuenta local enlazada
- La importacion inicial debe ejecutarse antes de abrir el canal a clientes reales
- Si un usuario ya existe en WordPress por email, el importador lo enlaza y actualiza metadatos

## Ruta recomendada de despliegue

1. staging
2. importar clientes
3. probar login de 3 a 5 clientes reales
4. probar 1 orden de contado
5. probar 1 orden a credito con cliente real controlado
6. probar 1 orden repetida para validar idempotencia
7. subir a produccion

## Manual tecnico extendido

Para arquitectura, n8n, DismalCRM, bot Telegram y app movil cliente, revisar:

- `docs/integrations/MANUAL_TECNICO_WORDPRESS_WOOCOMMERCE_DISMAL.md`
