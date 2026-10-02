#!/usr/bin/env sh
set -eu

SCRIPT_DIR=${0%/*}
[ "$SCRIPT_DIR" = "$0" ] && SCRIPT_DIR=.
ROOT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
COMPOSE_OVERRIDE="$ROOT_DIR/config/easypanel/docker-compose.override.yml"
TRAEFIK_TEMPLATE="$ROOT_DIR/config/easypanel/traefik-dismal.template.yml"

read_env_value() {
  KEY=$1
  [ -f "$ROOT_DIR/.env" ] || return 0
  sed -n "s/^${KEY}=//p" "$ROOT_DIR/.env" | tail -n 1 | sed 's/^"//;s/"$//'
}

EASYPANEL_PROXY_NETWORK=${EASYPANEL_PROXY_NETWORK:-$(read_env_value EASYPANEL_PROXY_NETWORK)}
EASYPANEL_TRAEFIK_SERVICE=${EASYPANEL_TRAEFIK_SERVICE:-$(read_env_value EASYPANEL_TRAEFIK_SERVICE)}
EASYPANEL_HTTP_ENTRYPOINT=${EASYPANEL_HTTP_ENTRYPOINT:-$(read_env_value EASYPANEL_HTTP_ENTRYPOINT)}
EASYPANEL_HTTPS_ENTRYPOINT=${EASYPANEL_HTTPS_ENTRYPOINT:-$(read_env_value EASYPANEL_HTTPS_ENTRYPOINT)}
EASYPANEL_CERT_RESOLVER=${EASYPANEL_CERT_RESOLVER:-$(read_env_value EASYPANEL_CERT_RESOLVER)}
EASYPANEL_TRAEFIK_CONFIG_DIR=${EASYPANEL_TRAEFIK_CONFIG_DIR:-$(read_env_value EASYPANEL_TRAEFIK_CONFIG_DIR)}

EASYPANEL_PROXY_NETWORK=${EASYPANEL_PROXY_NETWORK:-easypanel}
EASYPANEL_TRAEFIK_SERVICE=${EASYPANEL_TRAEFIK_SERVICE:-easypanel-traefik}
EASYPANEL_HTTP_ENTRYPOINT=${EASYPANEL_HTTP_ENTRYPOINT:-http}
EASYPANEL_HTTPS_ENTRYPOINT=${EASYPANEL_HTTPS_ENTRYPOINT:-https}
EASYPANEL_CERT_RESOLVER=${EASYPANEL_CERT_RESOLVER:-letsencrypt}
EASYPANEL_TRAEFIK_CONFIG_DIR=${EASYPANEL_TRAEFIK_CONFIG_DIR:-/etc/easypanel/traefik/config}
TRAEFIK_TARGET="$EASYPANEL_TRAEFIK_CONFIG_DIR/dismal.yml"

fail() {
  printf '\nERROR: %s\n' "$1" >&2
  exit 1
}

compose() {
  docker compose \
    -f "$ROOT_DIR/docker-compose.yml" \
    -f "$COMPOSE_OVERRIDE" \
    "$@"
}

wait_for_http() {
  SERVICE=$1
  URL=$2
  LABEL=$3
  ATTEMPT=0

  printf 'Esperando %s...\n' "$LABEL"
  while [ "$ATTEMPT" -lt 40 ]; do
    if compose exec -T "$SERVICE" wget -q -O /dev/null "$URL"; then
      printf '%s disponible.\n' "$LABEL"
      return 0
    fi
    ATTEMPT=$((ATTEMPT + 1))
    sleep 3
  done

  fail "$LABEL no respondio despues de 120 segundos."
}

wait_for_public_http() {
  URL=$1
  LABEL=$2
  ATTEMPT=0

  printf 'Verificando %s en Internet...\n' "$LABEL"
  while [ "$ATTEMPT" -lt 30 ]; do
    if curl --fail --silent --show-error --max-time 10 "$URL" >/dev/null; then
      printf '%s disponible publicamente.\n' "$LABEL"
      return 0
    fi
    ATTEMPT=$((ATTEMPT + 1))
    sleep 3
  done

  printf '\nEstado de Dismal al fallar la publicacion:\n' >&2
  compose ps >&2 || true
  printf '\nUltimos eventos del backend:\n' >&2
  compose logs --tail=80 backend >&2 || true
  printf '\nUltimos eventos del proxy de Easypanel:\n' >&2
  docker service logs --tail=80 "$EASYPANEL_TRAEFIK_SERVICE" >&2 || true
  fail "$LABEL no respondio publicamente despues de 90 segundos."
}

command -v docker >/dev/null 2>&1 || fail "Docker no esta instalado."
command -v curl >/dev/null 2>&1 || fail "curl no esta instalado."
docker compose version >/dev/null 2>&1 || fail "Docker Compose no esta disponible."
docker info >/dev/null 2>&1 || fail "El usuario actual no puede usar Docker."

[ -f "$COMPOSE_OVERRIDE" ] || fail "No existe $COMPOSE_OVERRIDE."
[ -f "$TRAEFIK_TEMPLATE" ] || fail "No existe $TRAEFIK_TEMPLATE."

docker network inspect "$EASYPANEL_PROXY_NETWORK" >/dev/null 2>&1 ||
  fail "No existe la red $EASYPANEL_PROXY_NETWORK de Easypanel."

ATTACHABLE=$(docker network inspect "$EASYPANEL_PROXY_NETWORK" --format '{{.Attachable}}')
[ "$ATTACHABLE" = "true" ] ||
  fail "La red $EASYPANEL_PROXY_NETWORK no es attachable. No se realizo ningun cambio."

docker service inspect "$EASYPANEL_TRAEFIK_SERVICE" >/dev/null 2>&1 ||
  fail "No existe el servicio $EASYPANEL_TRAEFIK_SERVICE."

NETWORK_ID=$(docker network inspect "$EASYPANEL_PROXY_NETWORK" --format '{{.Id}}')
TRAEFIK_NETWORKS=$(docker service inspect "$EASYPANEL_TRAEFIK_SERVICE" \
  --format '{{range .Spec.TaskTemplate.Networks}}{{println .Target}}{{end}}')
printf '%s\n' "$TRAEFIK_NETWORKS" | grep -Fx "$NETWORK_ID" >/dev/null 2>&1 ||
  fail "Traefik no esta conectado a la red $EASYPANEL_PROXY_NETWORK."

printf 'Validando Docker Compose...\n'
compose config --quiet

printf 'Construyendo y actualizando Dismal...\n'
compose up -d --build

printf 'Verificando servicios internos...\n'
compose ps
wait_for_http web http://127.0.0.1:3000/login "el frontend"
wait_for_http backend http://127.0.0.1:8080/actuator/health "el backend"

printf 'Instalando rutas de Traefik...\n'
mkdir -p "$EASYPANEL_TRAEFIK_CONFIG_DIR"
TMP_CONFIG=$(mktemp)
trap 'rm -f "$TMP_CONFIG"' EXIT HUP INT TERM

sed \
  -e "s/__HTTP_ENTRYPOINT__/$EASYPANEL_HTTP_ENTRYPOINT/g" \
  -e "s/__HTTPS_ENTRYPOINT__/$EASYPANEL_HTTPS_ENTRYPOINT/g" \
  -e "s/__CERT_RESOLVER__/$EASYPANEL_CERT_RESOLVER/g" \
  "$TRAEFIK_TEMPLATE" > "$TMP_CONFIG"

if [ -f "$TRAEFIK_TARGET" ]; then
  cp "$TRAEFIK_TARGET" "$TRAEFIK_TARGET.backup"
fi
install -m 0644 "$TMP_CONFIG" "$TRAEFIK_TARGET"

printf 'Reiniciando solamente el proxy de Easypanel...\n'
docker service update --force "$EASYPANEL_TRAEFIK_SERVICE" >/dev/null

printf 'Esperando la recarga de Traefik...\n'
ATTEMPT=0
RUNNING=
while [ "$ATTEMPT" -lt 30 ]; do
  RUNNING=$(docker service ls \
    --filter "name=$EASYPANEL_TRAEFIK_SERVICE" \
    --format '{{.Replicas}}')
  [ "$RUNNING" = "1/1" ] && break
  ATTEMPT=$((ATTEMPT + 1))
  sleep 2
done
[ "$RUNNING" = "1/1" ] || fail "Traefik no regreso al estado 1/1."

wait_for_public_http https://dismal.vip/login "el frontend de Dismal"
wait_for_public_http https://api.dismal.vip/actuator/health "la API de Dismal"

printf '\nDespliegue completado y verificado:\n'
printf '  https://dismal.vip/login\n'
printf '  https://api.dismal.vip/actuator/health\n'
