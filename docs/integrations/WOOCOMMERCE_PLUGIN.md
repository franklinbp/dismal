# WooCommerce Bridge Setup

## Ubicacion del plugin

`apps/wordpress/dismal-woocommerce-bridge`

## Ruta de despliegue recomendada

1. Configura el backend del VPS:
   - `WOOCOMMERCE_API_KEY=<clave_privada>`
   - `https://api.tudominio.com`

2. Copia el plugin al hosting compartido:
   - `wp-content/plugins/dismal-woocommerce-bridge`

3. Activa el plugin en WordPress

4. Configura en `WooCommerce > Dismal Bridge`:
   - API Base URL
   - Integration Key
   - estados de orden que disparan sincronizacion
   - roles WordPress que equivalen a `DISTRIBUTOR`

5. Ejecuta `Probar conexion con Dismal`

6. Mapea cada producto WooCommerce con su `Dismal Software ID`

7. Ejecuta una compra de prueba

## Flujo operativo

1. WooCommerce confirma el pago
2. El plugin arma el payload con cliente, metodo de pago e items
3. El VPS recibe `POST /api/public/integrations/woocommerce/order-paid`
4. Dismal crea venta, confirma, registra pago y evita duplicados
5. WooCommerce guarda `saleId` y estado de sincronizacion

## Manual tecnico extendido

Ver:

`docs/integrations/MANUAL_TECNICO_WORDPRESS_WOOCOMMERCE_DISMAL.md`

## Hardening recomendado

- Usa una API key distinta para `staging` y `production`
- Restringe el backend a HTTPS
- Monitorea logs de Nginx y aplicacion
- Mantiene copia de seguridad de WordPress y PostgreSQL antes del primer go-live
- Prueba rollback del plugin antes de publicar en produccion
