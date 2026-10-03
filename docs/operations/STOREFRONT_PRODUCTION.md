# Produccion de Dismal Ecuador

## Arquitectura

```text
dismal.vip / www.dismal.vip -> panel administrativo en el VPS
api.dismal.vip              -> API Spring Boot en el VPS
dismalec.com                -> tienda publica en hosting compartido
```

Dismal opera exclusivamente en Ecuador, usa USD y admite dos tipos de producto:

- fisico: controla stock disponible, reserva, despacho y seguimiento;
- digital: controla licencias o activaciones y su entrega.

El catalogo publico conserva visibles los productos sin stock para informar al cliente, pero el backend impide vender cantidades que no estan disponibles.

## Variables del VPS

```env
CORS_ALLOWED_ORIGINS=https://dismalec.com,https://www.dismalec.com,https://dismal.vip,https://www.dismal.vip
ACCOUNT_EC_STOREFRONT_URL=https://dismalec.com
ACCOUNT_PE_STOREFRONT_URL=https://dismalec.com
DISMAL_CRM_WHATSAPP_ID_BY_COUNTRY=EC:34
```

`ACCOUNT_PE_STOREFRONT_URL` se conserva temporalmente por compatibilidad con datos y versiones anteriores; no habilita ventas en Peru.

## Actualizar el VPS

Antes de actualizar, confirme que esta en `~/dismal`, que los puertos publicados son `9002` y `6002`, y que las variables secretas permanecen en `.env`.

```bash
cd ~/dismal
mkdir -p ~/backups/dismal
docker compose exec -T db pg_dump -U dismal -d dismal_db | gzip > ~/backups/dismal/dismal-before-update.sql.gz
git pull --ff-only
docker compose -f docker-compose.yml -f config/easypanel/docker-compose.override.yml up -d --build backend web
docker compose ps
curl -sS http://127.0.0.1:9002/actuator/health
docker compose exec -T web sh -lc 'echo "$BASE_API_URL"; getent hosts dismal-api'
```

La respuesta de salud debe indicar `UP`; el contenedor web debe mostrar `http://dismal-api:8080`. Ese alias exclusivo evita que Dismal utilice el backend de Maleskin.

## Publicar la tienda en el hosting compartido

Compile fuera del hosting:

```bash
cd apps/storefront
npm install
npm run build:ec
npm run verify:build
```

Suba el contenido de `apps/storefront/dist/ecuador/` al document root de `dismalec.com`, incluyendo `.htaccess`. No suba la carpeta `ecuador` como un nivel adicional.

## Verificacion posterior

- Iniciar sesion en `https://dismal.vip` y confirmar que no aparecen datos de Maleskin.
- Crear o editar un producto fisico con SKU, marca y stock.
- Crear o editar un producto digital y cargar sus activaciones/licencias.
- Confirmar que ambos aparecen en `https://dismalec.com`, incluso con disponibilidad cero.
- Confirmar que un pedido fisico exige datos de entrega y que un pedido digital no los exige.
- Confirmar que una solicitud con pais distinto de `EC` recibe HTTP 400.
