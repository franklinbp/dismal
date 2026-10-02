# Analisis tecnico de Facturacion y ruta de migracion

## Objetivo

Este documento resume la logica principal encontrada en la carpeta `Facturacion` y define como aprovechar sus partes importantes sin mezclar codigo legacy directamente dentro de Dismal.

La necesidad actual de Dismal no es emitir facturas electronicas. Lo importante a reutilizar como conocimiento es:

- Flujo de venta.
- Pagos y cuentas por cobrar.
- Caja/bancos/movimientos.
- Estructura operativa para una futura plataforma de facturacion escalable.

## Resumen arquitectonico de Facturacion

`Facturacion` es una aplicacion PHP tradicional con MariaDB. Usa un patron MVC simple:

- `index.php`: entrada principal, carga sesion, CSRF y vistas.
- `controller/`: clases que exponen operaciones de negocio.
- `model/`: acceso a datos por PDO y stored procedures.
- `view/`: pantallas PHP.
- `web/ajax/`: endpoints AJAX para operaciones del sistema.
- `DB/ecuadorventa.sql`: esquema, tablas, vistas y stored procedures.
- `sri/`: generacion, firma y envio de comprobantes electronicos al SRI.
- `src/Mike42/Escpos`: impresion de tickets.

La logica fuerte vive en base de datos, especialmente en stored procedures. La aplicacion PHP actua como coordinador entre UI, procedimientos SQL, impresion, correo y SRI.

## Flujo de venta en Facturacion

La venta se ejecuta desde `web/ajax/ajxventa.php`:

1. Recibe datos del formulario POS:
   - Cliente.
   - Tipo de comprobante.
   - Tipo de pago.
   - Totales: subtotal, IVA, exento, retenido, descuento y total.
   - Lista de productos en `stringdatos`.
2. Llama a `Venta::Insertar_Venta`.
3. Por cada producto llama a `Venta::Insertar_DetalleVenta`.
4. Si corresponde, finaliza la venta y genera movimientos de inventario/caja.
5. Puede imprimir ticket, generar comprobante o alimentar SRI.

La lista de productos llega en un formato delimitado:

```text
idproducto|cantidad|precio_unitario|exento|descuento|fecha_vence|importe
```

### Lectura para Dismal

Dismal ya tiene una separacion mas limpia:

- `sales`: venta.
- `sale_items`: detalle.
- `payments`: pagos.
- `accounts_receivable`: cuentas por cobrar.
- `licenses`: inventario/licencias.

Por tanto, no conviene migrar los stored procedures. Conviene migrar la regla de negocio, no el codigo.

## Pagos y cobros

En Facturacion, el pago esta amarrado al flujo de venta y a caja:

- Venta pagada: genera entrada inmediata.
- Venta a credito: queda pendiente y aparece en creditos/cobros.
- Abonos: reducen saldo pendiente.
- Caja: registra ingresos/egresos y cortes.

En Dismal, esto debe quedar asi:

- Una venta confirmada crea AR si queda saldo.
- Un pago se registra en `payments`.
- Cada pago actualiza `accounts_receivable`.
- Si el pago cubre el total, la venta pasa a `PAID`.
- Si es parcial, queda `CONFIRMED` con balance pendiente.
- Caja/banco debe ser un modulo separado de movimientos financieros, no dentro de venta.

## Modelo recomendado para Dismal

### Entidades minimas

- `Sale`: venta comercial.
- `SaleItem`: productos/licencias vendidos.
- `Payment`: pago recibido.
- `AccountsReceivable`: saldo pendiente.
- `CashAccount`: caja fisica, banco, billetera o pasarela.
- `CashMovement`: ingreso, egreso, transferencia, ajuste.
- `BankAccount`: cuenta bancaria o canal financiero.
- `BankTransaction`: movimiento bancario importado o registrado.
- `Reconciliation`: conciliacion entre pago y banco/caja.

### Reglas recomendadas

- Venta no debe registrar caja directamente.
- Pago genera movimiento financiero.
- Movimiento financiero puede estar conciliado o pendiente.
- Una venta puede tener varios pagos.
- Un pago puede tener metodo: `CASH`, `TRANSFER`, `CARD`, `CREDIT`.
- Credito no es pago. Credito es condicion de venta y genera cuenta por cobrar.
- Banco/caja debe tener auditoria: quien registro, fecha, referencia y observacion.

## Bancos y caja

Facturacion maneja caja de forma operativa. Para Dismal conviene hacerlo mas profesional:

1. Crear catalogo de cuentas financieras:
   - Caja local.
   - Banco Pichincha.
   - Banco Guayaquil.
   - PayPal, Stripe, PayPhone u otra pasarela.
2. Al registrar pago, seleccionar cuenta destino.
3. Crear movimiento financiero automatico.
4. Permitir conciliacion manual:
   - Pendiente.
   - Conciliado.
   - Observado.
5. Reportes:
   - Ingresos por metodo.
   - Ingresos por cuenta.
   - Cuentas por cobrar.
   - Flujo diario.
   - Ventas vs cobros.

## Facturacion electronica / SRI

Aunque ahora Dismal no necesita facturar, `Facturacion` tiene una logica util para futuro:

- Genera XML.
- Firma XML con Java/JAR.
- Envia a SRI por SOAP.
- Consulta autorizacion.
- Guarda autorizados, no autorizados y rechazados.
- Envia correo con PDF/XML.

No se recomienda copiar este modulo tal cual. La ruta profesional seria crear un microservicio fiscal aislado:

- API REST propia.
- Cola de trabajos para firma/envio.
- Estados claros: `DRAFT`, `SIGNED`, `SENT`, `AUTHORIZED`, `REJECTED`.
- Almacenamiento de XML/PDF en storage controlado.
- Certificados y claves fuera del repo.
- Auditoria completa.

## Plataforma recomendada para nueva version comercial

Para comercializar una plataforma de facturacion escalable, recomiendo:

- Backend: .NET 8/9 o Java Spring Boot.
- Frontend: React/Next.js.
- Base de datos: PostgreSQL.
- Cola: Redis + workers, o RabbitMQ si crece.
- Storage: S3 compatible para XML/PDF.
- Autenticacion: JWT + refresh tokens + roles.
- Multiempresa desde el inicio.
- Auditoria obligatoria.
- Docker y despliegue con CI/CD.

Si el objetivo es migrar hacia Visual Studio/.NET, la arquitectura recomendada seria:

- `Billing.Api`: API REST.
- `Billing.Application`: casos de uso.
- `Billing.Domain`: entidades y reglas.
- `Billing.Infrastructure`: EF Core, SRI, email, storage.
- `Billing.Worker`: procesos de firma/envio/consulta.
- `Billing.Web`: panel administrativo.

## Que migrar de Facturacion

Migrar como conocimiento:

- Flujo POS de venta.
- Manejo de detalle de productos.
- Concepto de credito/abono.
- Caja/corte diario.
- Reportes operativos.
- Flujo SRI como referencia.

No migrar directamente:

- Stored procedures grandes.
- Concatenacion de strings para detalles.
- Endpoints PHP directos por GET.
- Archivos XML/PDF expuestos directamente.
- Certificados o claves en carpetas publicas.

## Prioridad para Dismal

1. Consolidar ventas, pagos y AR en Dismal.
2. Crear modulo caja/bancos.
3. Conectar pagos de venta web, movil y WooCommerce al mismo flujo.
4. Agregar conciliacion.
5. Solo despues disenar facturacion electronica como modulo separado.

