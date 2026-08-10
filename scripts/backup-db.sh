#!/usr/bin/env bash
#
# backup-db.sh — Backup diario de PostgreSQL con upload a Google Drive.
#
# Uso:
#   backup-db.sh                  # backup diario (retencion 14 dias)
#
# Requisitos en el VPS:
#   apt-get install -y docker.io
#   rclone (instalar SIN snap — la version snap corre confinada y no puede
#   leer rutas fuera de $HOME como /var/backups, lo que rompe el upload con
#   "error reading source root directory"):
#     curl https://rclone.org/install.sh | sudo bash
#
# Variables de entorno (definir en /etc/floristeria/backup.env, chmod 600):
#   DB_CONTAINER_NAME     — (opcional) fija el nombre EXACTO del contenedor.
#                            Si se omite, se resuelve dinamicamente buscando
#                            un contenedor corriendo cuyo nombre empiece con
#                            DB_CONTAINER_PATTERN. Dejar SIN fijar salvo que
#                            el nombre real no matchee el patron.
#   DB_CONTAINER_PATTERN  — Prefijo para buscar el contenedor postgres
#                            (default: postgres-floristeria)
#   DB_USER              — Usuario de la base
#   DB_NAME              — Nombre de la base
#   BACKUP_RETENTION_DAYS — Dias a conservar en Drive (default: 14)
#   GDRIVE_REMOTE_NAME   — nombre del remote configurado en rclone (el que tu elijas,
#                          ej. PruebaTao, floristeria-drive, etc.)
#   GDRIVE_FOLDER        — carpeta destino dentro del Drive (ej. floristeria-backups)
#
# Configuracion de Google Drive en rclone (documentacion):
# -----------------------------------------------------------------------------
# 1. Crear/usar una cuenta de Google dedicada para backups (no personal).
# 2. Crear un Client ID y Client Secret PROPIOS en Google Cloud Console
#    (APIs & Services -> Credentials -> OAuth client ID -> Desktop app).
#    NO usar el client_id compartido por defecto de rclone — con credenciales
#    propias evitas los limites de cuota compartidos entre todos los usuarios
#    de rclone en el mundo.
# 3. Configurar rclone en el VPS (interactivo, una sola vez):
#      rclone config
#        -> n (new remote)
#        -> name: <el nombre que elijas, debe coincidir con GDRIVE_REMOTE_NAME>
#        -> storage: Google Drive (buscar "drive" en la lista)
#        -> client_id:     <tu Client ID de Google Cloud>
#        -> client_secret: <tu Client Secret de Google Cloud>
#        -> scope: drive    (acceso completo)
#        -> root_folder_id: (dejar en blanco)
#        -> service_account_file: (dejar en blanco)
#    Completar la autorizacion OAuth. Si el VPS no tiene navegador: responder
#    "n" a "Use web browser to automatically authenticate", correr
#    `rclone authorize "drive" <client_id> <client_secret>` en una maquina CON
#    navegador (mismas credenciales), y pegar el token resultante de vuelta
#    en la sesion del VPS que quedo esperando.
#    Confirmar "Configure this as a Shared Drive? No" (a menos que uses Google
#    Workspace con Drive compartido).
# 4. Validar:
#      rclone about <GDRIVE_REMOTE_NAME>:
#      rclone lsd <GDRIVE_REMOTE_NAME>:
# 5. Crear la carpeta destino:
#      rclone mkdir <GDRIVE_REMOTE_NAME>:floristeria-backups
# 6. El archivo de config resultante vive en ~/.config/rclone/rclone.conf
#    (contiene el refresh token). Respaldar este archivo FUERA del VPS en el
#    gestor de secretos off-VPS que ya definieron: si se pierde el VPS hay que
#    reconfigurar rclone desde cero, perdiendo el token.
# 7. Crear /etc/floristeria/backup.env (chmod 600, propietario root):
#      GDRIVE_REMOTE_NAME=<el nombre que usaste en el paso 3>
#      GDRIVE_FOLDER=floristeria-backups
#      # DB_CONTAINER_NAME=db_floristeria_prod   # opcional: solo si el nombre real no matchea el patron
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
#      rclone copy <GDRIVE_REMOTE_NAME>:floristeria-backups/2026/07/25/floristeria_20260725_030000.sql.gz /tmp/
# 2. Detener el backend para no tener writes conflictivos:
#      docker stop backend_floristeria_prod
# 3. Restaurar la base (sobreescribe datos existentes — usar con cuidado):
#      gunzip -c /tmp/floristeria_20260725_030000.sql.gz | \
#        docker exec -i db_floristeria_prod psql -U admin -d floristeria_db
# 4. Reiniciar backend y verificar:
#      docker start backend_floristeria_prod
#      curl -fsS http://localhost:8080/actuator/health
# 5. Probar en ambiente de pruebas primero (VPS staging o instancia local):
#      - Restaurar a un contenedor postgres paralelo en puerto 5433.
#      - Conectar el backend al staging y verificar pedidos, inventarios, etc.
#    NOTA: Un backup no probado es un backup que no se sabe si sirve.
#          Ejecutar el restore al menos UNA vez mensual como simulacro.
#
# Retencion en Drive:
# -----------------------------------------------------------------------------
# rclone no hace retencion automatica; este script borra en Drive los archivos
# anteriores a BACKUP_RETENTION_DAYS dias usando `rclone delete --min-age`
# (NO `rclone purge`, que ignora --min-age y borraria TODO el arbol). La
# rotacion apunta al folder completo para que --min-age pueda encontrar
# backups viejos alojados en subcarpetas de fecha distintas a la de hoy.
# Verificado con prueba real: `rclone delete ... --min-age 14d --dry-run -vv`
# selecciona solo archivos viejos y respeta los recientes.
#
# Si un dia se falla a instalar rclone via snap en vez del instalador oficial:
# el upload puede fallar con "error reading source root directory" porque el
# snap corre confinado y no puede leer rutas fuera de $HOME (LOCAL_DIR vive en
# /var/backups, fuera de $HOME). La solucion es desinstalar el snap y usar el
# instalador oficial (ver "Requisitos" arriba) — no hay que tocar la logica
# de este script para eso.
# -----------------------------------------------------------------------------

set -euo pipefail

ENV_FILE="${ENV_FILE:-/etc/floristeria/backup.env}"
if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  set -a; . "$ENV_FILE"; set +a
fi

: "${GDRIVE_REMOTE_NAME:?Falta GDRIVE_REMOTE_NAME}"
: "${GDRIVE_FOLDER:?Falta GDRIVE_FOLDER}"
: "${DB_USER:?Falta DB_USER}"
: "${DB_NAME:=floristeria_db}"  # fall back a nombre historico
: "${BACKUP_RETENTION_DAYS:=14}"

# Resolucion dinamica del contenedor de Postgres.
#
# Coolify le agrega a cada contenedor un sufijo (uuid/hash de deployment) que
# cambia en cualquier recreate del servicio (redeploy, rebuild de imagen,
# restart por healthcheck fallido), aunque no lo dispares manualmente. Si
# DB_CONTAINER_NAME queda fijo en backup.env, el backup deja de encontrar el
# contenedor en silencio (exit 1) hasta que alguien revisa el log a mano.
#
# En vez de fijar el nombre completo, se resuelve por patron en cada
# corrida. DB_CONTAINER_PATTERN es configurable en backup.env por si el
# prefijo cambia; por defecto "postgres-floristeria".
: "${DB_CONTAINER_PATTERN:=postgres-floristeria}"

if [[ -z "${DB_CONTAINER_NAME:-}" ]]; then
  mapfile -t _matches < <(docker ps --format '{{.Names}}' | grep -E "^${DB_CONTAINER_PATTERN}")
  if [[ "${#_matches[@]}" -eq 0 ]]; then
    echo "ERROR: no se encontro ningun contenedor corriendo que matchee '${DB_CONTAINER_PATTERN}'." >&2
    exit 4
  elif [[ "${#_matches[@]}" -gt 1 ]]; then
    echo "ERROR: se encontraron varios contenedores que matchean '${DB_CONTAINER_PATTERN}', fija DB_CONTAINER_NAME en backup.env:" >&2
    printf '  %s\n' "${_matches[@]}" >&2
    exit 4
  fi
  DB_CONTAINER_NAME="${_matches[0]}"
fi

echo "[$(date -Is)] Usando contenedor: ${DB_CONTAINER_NAME}"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DATE_PATH=$(date +%Y/%m/%d)
LOCAL_DIR="${LOCAL_DIR:-/var/backups/floristeria}"
LOCAL_FILE="${LOCAL_DIR}/floristeria_${TIMESTAMP}.sql.gz"
GDRIVE_BASE="${GDRIVE_REMOTE_NAME}:${GDRIVE_FOLDER}"
GDRIVE_PATH="${GDRIVE_BASE}/${DATE_PATH}/$(basename "$LOCAL_FILE")"

mkdir -p "$LOCAL_DIR"

# Limpieza automatica del archivo temporal local incluso si el script falla
# a mitad de camino (ej. durante el upload).
cleanup() {
  local exit_code=$?
  if [[ -f "$LOCAL_FILE" && "$exit_code" -ne 0 ]]; then
    echo "[$(date -Is)] Limpieza: eliminando archivo temporal tras fallo (exit ${exit_code}): ${LOCAL_FILE}"
    rm -f "$LOCAL_FILE"
  fi
}
trap cleanup EXIT

echo "[$(date -Is)] Iniciando backup de PostgreSQL..."

# 1. pg_dump del contenedor, comprimido al vuelo
docker exec "$DB_CONTAINER_NAME" \
  pg_dump -U "$DB_USER" -d "$DB_NAME" --clean --if-exists --no-owner --no-privileges \
  | gzip -9 > "$LOCAL_FILE"

LOCAL_SIZE=$(stat -c%s "$LOCAL_FILE" 2>/dev/null || stat -f%z "$LOCAL_FILE")
echo "[$(date -Is)] Backup local creado: ${LOCAL_FILE} (${LOCAL_SIZE} bytes)"

# 2. Upload a Google Drive con rclone
if ! command -v rclone >/dev/null 2>&1; then
  echo "ERROR: rclone no instalado. Instalar con: curl https://rclone.org/install.sh | sudo bash" >&2
  exit 2
fi

rclone copy "$LOCAL_FILE" "${GDRIVE_BASE}/${DATE_PATH}/" \
  --transfers 2 --quiet

if ! rclone ls "${GDRIVE_BASE}/${DATE_PATH}/$(basename "$LOCAL_FILE")" >/dev/null 2>&1; then
  echo "ERROR: fallo verificacion upload a Drive" >&2
  exit 3
fi
echo "[$(date -Is)] Upload Drive OK: ${GDRIVE_PATH}"

# 3. Rotacion local: mantener solo BACKUP_RETENTION_DAYS dias
find "$LOCAL_DIR" -name "floristeria_*.sql.gz" -mtime +"$BACKUP_RETENTION_DAYS" -delete

# 4. Rotacion en Drive: eliminar archivos mas antiguos que BACKUP_RETENTION_DAYS
#    (rclone purge ignora --min-age y borraria TODO el arbol; se usa `delete`.
#    Apuntar al folder completo, no a la ruta del dia, para que --min-age
#    encuentre los backups viejos alojados en otras subcarpetas de fecha.)
#    Un fallo aqui NO debe tumbar el backup (ya se subio correctamente arriba),
#    pero SI debe quedar visible en el log — antes se ocultaba por completo con
#    2>/dev/null, lo que hubiera dejado crecer el storage sin limite en
#    silencio ante cualquier problema de token/cuota.
if ! rclone delete "${GDRIVE_BASE}" \
  --min-age "${BACKUP_RETENTION_DAYS}d" \
  --rmdirs=false; then
  echo "[$(date -Is)] WARNING: fallo la rotacion en Google Drive (backup del dia OK, revisar manualmente)" >&2
fi

echo "[$(date -Is)] Backup completado."