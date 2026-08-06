#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SQL_DIR="$PROJECT_ROOT/script/sql/sqlserver"
ENV_FILE="${ENV_FILE:-$SCRIPT_DIR/.env}"

env_value() {
  local name="$1"
  local default="${2:-}"
  local file_value=""
  if [[ -f "$ENV_FILE" ]]; then
    file_value="$(grep -E "^${name}=" "$ENV_FILE" | tail -n 1 | cut -d '=' -f 2- || true)"
  fi
  if [[ -n "${!name:-}" ]]; then
    printf '%s' "${!name}"
  elif [[ -n "$file_value" ]]; then
    printf '%s' "$file_value"
  else
    printf '%s' "$default"
  fi
}

command -v sqlcmd >/dev/null 2>&1 || { echo "Missing required command: sqlcmd" >&2; exit 1; }

SQL_SERVER="$(env_value FX_SQLSERVER_HOST "")"
SQL_USER="$(env_value FX_SQLSERVER_USER sa)"
SQL_PASSWORD="$(env_value FX_SQLSERVER_PASSWORD "")"
DATABASE="$(env_value FX_ENTERPRISE_DATABASE enterprise)"

if [[ -z "$SQL_SERVER" || -z "$SQL_PASSWORD" ]]; then
  echo "FX_SQLSERVER_HOST and FX_SQLSERVER_PASSWORD are required." >&2
  exit 1
fi

echo "==> Ensuring database $DATABASE exists"
sqlcmd -S "$SQL_SERVER" -U "$SQL_USER" -P "$SQL_PASSWORD" -C -b -d master \
  -Q "IF DB_ID(N'$DATABASE') IS NULL EXEC(N'CREATE DATABASE [$DATABASE] COLLATE Chinese_PRC_CI_AS');"

echo "==> Running enterprise initialization data"
(cd "$SQL_DIR" && sqlcmd -S "$SQL_SERVER" -U "$SQL_USER" -P "$SQL_PASSWORD" -C -b -d "$DATABASE" -i "carbon_enterprise_init.sql")
(cd "$SQL_DIR" && sqlcmd -S "$SQL_SERVER" -U "$SQL_USER" -P "$SQL_PASSWORD" -C -b -d "$DATABASE" -i "session-timeout-12h.sql")

echo "Enterprise SQL Server initialization complete."
