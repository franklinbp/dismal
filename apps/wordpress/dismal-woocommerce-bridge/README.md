# Dismal WooCommerce Bridge

Plugin de WordPress/WooCommerce para sincronizar ordenes pagadas con el backend de Dismal en el VPS.

## Incluye

- Configuracion de URL base y API key del backend
- Mapeo de `softwareId` por producto WooCommerce
- Sincronizacion automatica cuando la orden entra en `processing` o `completed`
- Deteccion de clientes distribuidores por rol de WordPress
- Actualizacion del perfil WooCommerce usando los datos de la orden sincronizada
- Importacion inicial de clientes desde Dismal a WooCommerce
- Enlace de usuarios WooCommerce con `dismal_user_id`
- Pasarela `Credito Dismal` para clientes importados con credito disponible
- Prueba de conexion desde `WooCommerce > Dismal Bridge`
- Proteccion para no reenviar ordenes ya sincronizadas salvo reintento manual
- Reintento manual desde la ficha de la orden
- Trazabilidad por metadatos de la orden

## Instalacion

1. Copia la carpeta `dismal-woocommerce-bridge` dentro de `wp-content/plugins/`
2. Activa el plugin en WordPress
3. Ve a `WooCommerce > Dismal Bridge`
4. Configura:
   - `API Base URL`: `https://api.tudominio.com`
   - `Integration Key`: el mismo valor de `integrations.woocommerce.api-key` en el backend
   - `Paid Order Statuses`
   - `Distributor WordPress Roles`
5. Presiona `Probar conexion con Dismal`
6. En cada producto WooCommerce completa `Dismal Software ID`
7. Usa la seccion `Importar clientes desde Dismal` para crear o enlazar clientes existentes antes de abrir el canal
8. Activa la pasarela `Credito Dismal` en `WooCommerce > Ajustes > Pagos` si venderas a credito

## Metadatos de orden

El plugin guarda:

- `_dismal_external_order_id`
- `_dismal_sale_id`
- `_dismal_sync_status`
- `_dismal_sync_message`
- `_dismal_last_sync_at`

## Metadatos de cliente

El importador guarda en WordPress:

- `_dismal_user_id`
- `_dismal_customer_type`
- `_dismal_enabled`
- `_dismal_has_credit`
- `_dismal_credit_limit`
- `_dismal_credit_used`
- `_dismal_credit_days`
- `_dismal_last_sync_at`

## Flujo recomendado

1. Configura la integracion y valida una orden de prueba en staging
2. Importa clientes existentes desde `Dismal`
3. Pide a los clientes entrar con su `email` y clave de `Dismal`
4. Revisa que las ordenes nuevas lleguen a `Dismal` con `saleId`
5. Para clientes con credito, valida que el checkout muestre `Credito Dismal` solo si el credito disponible alcanza el total del carrito

## Recomendacion operativa

Prueba primero en staging con una orden real o sandbox y valida:

1. Creacion de venta
2. Registro de pago
3. Idempotencia al reenviar la misma orden
4. Mapeo correcto de productos y cliente
5. Importacion inicial de clientes sin duplicados por email
6. Compra a credito sin registrar pago inmediato y con AR abierto en `Dismal`

## Manual tecnico completo

Consulta:

`docs/integrations/MANUAL_TECNICO_WORDPRESS_WOOCOMMERCE_DISMAL.md`
