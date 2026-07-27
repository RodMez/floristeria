#!/usr/bin/env bash
#
# backup-db.sh — Backup diario de PostgreSQL con upload a Backblaze B2.
#
# Uso:
#   backup-db.sh                  # backup diario (retencion 14 dias)
#   backup-db.sh --full           # backup completo sin rotacion local
#
# Requisitos en el VPS:
#   apt-get install -y docker.io rclone
#
# Variables de entorno (definir en /etc/floristeria/backup.env, chmod 600):
#   B2_KEY_ID            — Application Key ID de Backblaze B2
#   B2_APPLICATION_KEY   — Application Key (secret)
#   B2_BUCKET_NAME       — Nombre del bucket B2 (ej. floristeria-backups)
#   DB_CONTAINER_NAME    — Nombre del contenedor postgres (default: db_floristeria_prod)
#   DB_USER              — Usuario de la base
#   DB_NAME              — Nombre de la base
#   BACKUP_RETENTION_DAYS — Dias a conservar en B2 (default: 14)
#
# Creacion del bucket y application key (documentacion):
# -----------------------------------------------------------------------------
# 1. Crear cuenta en https://backblaze.com y verificar email.
# 2. En el panel B2, "Buckets" → "Create a Bucket":
#      - Name: floristeria-backups  (o el que prefieras; sincronizar con B2_BUCKET_NAME)
#      - Files are: Private
#      - Encryption: optional (recomendado para PII de clientes)
# 3. "Account" → "App Keys" → "Add a New Application Key":
#      - Name: floristeria-backup
#      - Allow access to Bucket(s): floristeria-backups
#      - Capabilities: listBuckets, readFiles, writeFiles, deleteFiles
#    Al crearla, B2 entrega:
#      keyID      -> B2_KEY_ID
#      applicationKey -> B2_APPLICATION_KEY (se muestra UNA sola vez)
# 4. Configurar rclone en el VPS (interactivo, una sola vez):
#      rclone config
#        -> n (new remote)
#        -> name: b2
#        -> storage: Backblaze B2
#        -> account/key id: <B2_KEY_ID>
#        -> application key: <B2_APPLICATION_KEY>
#    Validar:
#      rclone lsd b2:floristeria-backups
# 5. Crear /etc/floristeria/backup.env (chmod 600, propietario root):
#      B2_KEY_ID=<...>
#      B2_APPLICATION_KEY=<...>
#      B2_BUCKET_NAME=floristeria-backups
#      DB_CONTAINER_NAME=db_floristeria_prod
#      DB_USER=admin
#      DB_NAME=floristeria_db
#      BACKUP_RETENTION_DAYS=14
#
# Cron (en crontab de root, `crontab -e`):
#   0 3 * * * /opt/floristeria/scripts/backup-db.sh >> /var/log/floristeria-backup.log 2>&1
#
# Restauracion (probar en ambiente de pruebas ANTES de confiar en prod):
# -----------------------------------------------------------------------------
# 1. Descargar el archivo del dia:
#      rclone copy b2:floristeria-backups/2026/07/25/floristeria_20260725_030000.sql.gz /tmp/
# 2. Detener el backend para no tener writes conflictivos:
#      docker stop backend_floristeria_prod
# 3. Restaurar la base (sobreescribe datos existentes — usar con cuidado):
#      gunzip -c /tmp/floristeria_20260725_030000.sql.gz | \
#        docker exec -i db_floristeria_prod psql -U admin -d floristeria_db
# 4. Reiniciar backend y verificar:
#      docker start backend_floristeria_prod
#      curl -fsS http://localhost:8080/actuator/health
# 5. Probar en ambiente de pruebas primero (VPS staging o isntancia local):
#      - Restaurar a un contenedor postgres paralelo en puerto 5433.
#      - Conectar el backend al staging y verificar pedidos, inventarios, etc.
#    NOTA: Un backup no probado es un backup que no se sabe si sirve.
#          Ejecutar el restore al menos UNA vez mensual como simulacro.
#
# Retencion en B2:
# -----------------------------------------------------------------------------
# rclone no hace retencion automatica; este script borra en B2 los archivos
# anteriores a BACKUP_RETENTION_DAYS dias. Para reglas de retencion avanzadas
# (lifecycle rules), configurarlas en el panel B2 → Bucket → Lifecycle.
# -----------------------------------------------------------------------------

set -euo pipefail

ENV_FILE="${ENV_FILE:-/etc/floristeria/backup.env}"
if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  set -a; . "$ENV_FILE"; set +a
fi

: "${B2_KEY_ID:?Falta B2_KEY_ID}"
: "${B2_APPLICATION_KEY:?Falta B2_APPLICATION_KEY}"
: "${B2_BUCKET_NAME:?Falta B2_BUCKET_NAME}"
: "${DB_CONTAINER_NAME:=db_floristeria_prod}"
: "${DB_USER:?Falta DB_USER}"
: "${DB_NAME:=floristeria_db}"  # fall back a nombre historico
: "${BACKUP_RETENTION_DAYS:=14}"

TIMESTAMP=$(date -u +%Y%m%d_%H%M%S)
DATE_PATH=$(date -u +%Y/%m/%d)
LOCAL_DIR="${LOCAL_DIR:-/var/backups/floristeria}"
LOCAL_FILE="${LOCAL_DIR}/floristeria_${TIMESTAMP}.sql.gz"
B2_REMOTE="b2"
B2_PATH="${B2_REMOTE}:${B2_BUCKET_NAME}/${DATE_PATH}/$(basename "$LOCAL_FILE")"

mkdir -p "$LOCAL_DIR"

echo "[$(date -Is)] Iniciando backup de PostgreSQL..."

# 1. pg_dump del contenedor, comprimido al vuelo
docker exec "$DB_CONTAINER_NAME" \
  pg_dump -U "$DB_USER" -d "$DB_NAME" --clean --if-exists --no-owner --no-privileges \
  | gzip -9 > "$LOCAL_FILE"

LOCAL_SIZE=$(stat -c%s "$LOCAL_FILE" 2>/dev/null || stat -f%z "$LOCAL_FILE")
echo "[$(date -Is)] Backup local creado: ${LOCAL_FILE} (${LOCAL_SIZE} bytes)"

# 2. Upload a B2 con rclone
if ! command -v rclone >/dev/null 2>&1; then
  echo "ERROR: rclone no instalado. Instalar con: apt-get install -y rclone" >&2
  exit 2
fi

rclone copy "$LOCAL_FILE" "${B2_REMOTE}:${B2_BUCKET_NAME}/${DATE_PATH}/" \
  --transfers 2 --quiet

if ! rclone ls "${B2_REMOTE}:${B2_BUCKET_NAME}/${DATE_PATH}/$(basename "$LOCAL_FILE")" >/dev/null 2>&1; then
  echo "ERROR: fallo verificacion upload a B2" >&2
  exit 3
fi
echo "[$(date -Is)] Upload B2 OK: ${B2_PATH}"

# 3. Rotacion local: mantener solo BACKUP_RETENTION_DAYS dias
find "$LOCAL_DIR" -name "floristeria_*.sql.gz" -mtime +"$BACKUP_RETENTION_DAYS" -delete

# 4. Rotacion en B2: eliminar archivos mas antiguos que BACKUP_RETENTION_DAYS
rclone purge "${B2_REMOTE}:${B2_BUCKET_NAME}" \
  --min-age "${BACKUP_RETENTION_DAYS}d" \
  --rmdirs=false 2>/dev/null || true

echo "[$(date -Is)] Backup completado."
