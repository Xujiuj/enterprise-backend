#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ENV_FILE:-$SCRIPT_DIR/.env}"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.yml"
FRESH="${FRESH:-false}"
SKIP_BUILD="${SKIP_BUILD:-false}"
SKIP_DB_INIT="${SKIP_DB_INIT:-false}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing env file: $ENV_FILE. Copy deploy/.env.example to deploy/.env and configure it first." >&2
  exit 1
fi
command -v docker >/dev/null 2>&1 || { echo "Missing required command: docker" >&2; exit 1; }

if [[ "$SKIP_BUILD" != "true" ]]; then
  "$SCRIPT_DIR/build-image.sh" "$ENV_FILE"
fi

if [[ "$FRESH" == "true" && "$SKIP_DB_INIT" != "true" ]]; then
  ENV_FILE="$ENV_FILE" "$SCRIPT_DIR/init-sqlserver.sh"
fi

echo "==> Starting enterprise backend"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps
echo "Enterprise backend deployment complete."
