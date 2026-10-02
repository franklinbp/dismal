# Migracion a comercio fisico

## Objetivo

Dismal Distribuciones vende bienes fisicos. La disponibilidad se expresa en unidades, no en claves de licencia, y la compra termina cuando el pedido se entrega al cliente.

## Flujo objetivo

1. El cliente agrega productos con stock disponible.
2. Al crear la orden se reservan unidades de inventario.
3. Al aprobar el pago la orden pasa a `PREPARING`.
4. Bodega prepara y marca `READY_FOR_DISPATCH`.
5. Se registra transportista y guia, pasando a `SHIPPED`.
6. La confirmacion de entrega cambia el estado a `DELIVERED`.
7. Una cancelacion antes del despacho libera las unidades reservadas.

## Compatibilidad heredada

La tabla y las clases de licencias se conservan durante la primera fase para no romper modulos compartidos. Ninguna nueva funcionalidad de Dismal Distribuciones debe crear licencias digitales. La retirada definitiva se hara despues de migrar inventario, ventas, escritorio, Android, notificaciones y WooCommerce al flujo fisico.

## Siguiente fase

- Exponer SKU, dimensiones y stock en los DTO de catalogo y administracion.
- Implementar reserva atomica y liberacion de existencias.
- Incorporar direccion de envio al checkout publico.
- Crear operacion de bodega para preparacion, guia y entrega.
- Sustituir textos, correos y pantallas heredadas de licencias en todos los clientes.
