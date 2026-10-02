#!/usr/bin/env bash
set -euo pipefail

if [ -z "${DB_URL:-}" ]; then
  echo "DB_URL is required (e.g. jdbc:postgresql://localhost:5432/dismal_db)" >&2
  exit 1
fi

if [ -z "${DB_USER:-}" ] || [ -z "${DB_PASSWORD:-}" ]; then
  echo "DB_USER and DB_PASSWORD are required." >&2
  exit 1
fi

if ! command -v pg_dump >/dev/null 2>&1; then
  echo "pg_dump not found. Install PostgreSQL client tools." >&2
  exit 1
fi

OUTPUT="src/main/resources/db/migration/V1__baseline.sql"

echo "Generating schema-only migration at ${OUTPUT}..."
PGPASSWORD="${DB_PASSWORD}" pg_dump \
  --schema-only \
  --no-owner \
  --no-privileges \
  --dbname="${DB_URL#jdbc:}" \
  --username="${DB_USER}" \
  > "${OUTPUT}"

echo "Done. Review ${OUTPUT} before committing."
