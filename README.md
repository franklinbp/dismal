# Dismal Distribuciones

Plataforma omnicanal para la venta y distribucion de productos fisicos en Ecuador y Peru. Este proyecto nace como una replica independiente de Dismal y conserva su arquitectura multiaplicacion, pero cambia el dominio principal de licencias digitales a catalogo, inventario, preparacion, despacho y entrega fisica.

## Aplicaciones

- `apps/storefront` - tienda publica y flujo de compra
- `apps/web` - administracion web (Next.js)
- `apps/desktop` - operacion de escritorio (JavaFX)
- `apps/android` - operacion movil
- `src` - API y reglas de negocio (Spring Boot)

## Dominio fisico

La migracion `V23__physical_commerce_foundation.sql` incorpora SKU, codigo de barras, marca, inventario por cantidad, peso y dimensiones de empaque, ademas de la direccion y seguimiento del envio. Los estados de pedido contemplan preparacion, despacho y entrega.

## Ejecucion

Backend: `./mvnw spring-boot:run`

Tienda: `cd apps/storefront`, `npm install`, `npm run dev`

Administracion: `cd apps/web`, `npm install`, `npm run dev`

## Nota de migracion

Se conserva temporalmente el espacio de paquetes Java `com.dismal.distribuciones` para reducir el riesgo tecnico durante la separacion. Los modulos heredados de licencias permanecen aislados para poder retirar o transformar sus integraciones en una segunda fase sin romper ventas, cartera, precios, usuarios ni reportes.
