#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="${1:-$SCRIPT_DIR/.env}"

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

command -v mvn >/dev/null 2>&1 || { echo "Missing required command: mvn" >&2; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "Missing required command: docker" >&2; exit 1; }

REGISTRY="$(env_value REGISTRY local)"
IMAGE_NAMESPACE="$(env_value IMAGE_NAMESPACE fx)"
IMAGE_TAG="$(env_value IMAGE_TAG latest)"
IMAGE="$REGISTRY/$IMAGE_NAMESPACE/enterprise-backend:$IMAGE_TAG"

echo "==> Building enterprise backend jar"
(cd "$PROJECT_DIR" && mvn -pl ruoyi-admin -am clean package -DskipTests)

echo "==> Building image $IMAGE"
docker build -t "$IMAGE" -f "$PROJECT_DIR/ruoyi-admin/Dockerfile" "$PROJECT_DIR/ruoyi-admin"

echo "Built $IMAGE"
