# Dismal Storefront

Tienda de Dismal Distribuciones para Ecuador en `dismalec.com`. El hosting compartido sirve HTML, CSS, JavaScript e imagenes; la logica sensible permanece en `api.dismal.vip`.

## Funcionalidad actual

- Catalogo unico en USD para Ecuador.
- Productos fisicos con inventario y productos digitales con activaciones/licencias.
- Portada, catalogo y paginas permanentes de todos los productos, incluso los que temporalmente no tienen stock.
- Carrito persistente y checkout validado por backend.
- Cuenta de cliente creada durante la primera compra.
- Inicio de sesion para cliente final o distribuidor aprobado.
- Verificacion de correo, recuperacion y cambio seguro de contrasena.
- Perfil editable y solicitudes de distribuidor con aprobacion administrativa.
- Precio distribuidor protegido por autenticacion.
- Historial de ordenes y consulta privada de licencias entregadas.
- Transferencia bancaria en Ecuador con registro de referencia y estado `PAYMENT_REVIEW`.
- Revision administrativa idempotente que crea la venta, registra el pago y prepara el envio fisico o la activacion digital.
- Compra directa con cupo de credito para cuentas verificadas y previamente aprobadas.
- Cuentas por cobrar y consumo/liberacion del cupo gestionados por el backend.
- SEO, reglas de seguridad, cache y compresion para Apache.

## Builds

Los comandos se ejecutan localmente o en CI, nunca en el hosting compartido:

```bash
npm install
npm run build:ec
npm run build:all
npm run verify:build
```

Salidas:

```text
dist/ecuador/  -> contenido del document root de dismalec.com
```

Cada carpeta incluye `index.html`, `.htaccess`, `robots.txt`, `sitemap.xml`, `assets/` y las imagenes publicas. Se sube el contenido de la carpeta, no la carpeta contenedora. `verify:build` comprueba los archivos, metadatos, reglas de seguridad y rutas publicas antes de publicar.

Las secciones usan rutas reales (`/catalogo`, `/ofertas`, `/ayuda`, etc.). Apache las dirige a `index.html` mediante `.htaccess`, por lo que los enlaces directos y el boton Atrás del navegador funcionan sin instalar software en el hosting.

## API

`VITE_DISMAL_API_URL` es opcional. En un dominio real la tienda usa `https://api.dismal.vip`; en `localhost`, sin variable, usa el catalogo de demostracion.

Para conectar un backend local:

```env
VITE_DISMAL_API_URL=http://localhost:8080
```

Los botones de WhatsApp se habilitan solo cuando existe un numero configurado. Debe escribirse con codigo internacional y solo digitos:

```env
VITE_WHATSAPP_EC=593...
```

Telegram se habilita solamente cuando se configura el usuario publico del bot o canal, sin `@`:

```env
VITE_TELEGRAM_EC=dismal_ec
```

## Limite de responsabilidad

El frontend nunca confirma pagos, concede credito, calcula precios definitivos ni entrega licencias. Esa responsabilidad pertenece al backend central. La transferencia se valida manualmente en `Pedidos web`; la salida publica no debe anunciar pago automatico hasta configurar una pasarela real y verificar su webhook.

El procedimiento completo de salida esta en `docs/operations/STOREFRONT_PRODUCTION.md`.
