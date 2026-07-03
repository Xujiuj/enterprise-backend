#!/usr/bin/env bash
set -euo pipefail

SERVER_HOST="${FX_DEPLOY_HOST:-124.221.155.102}"
SSH_USER="${FX_DEPLOY_USER:-ubuntu}"
PASSWORD="${FX_DEPLOY_PASSWORD:-Test0000}"
REMOTE_PATH="${FX_ENTERPRISE_BACKEND_JAR:-/opt/fx/apps/enterprise-backend/app.jar}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAR_PATH="$REPO_ROOT/ruoyi-admin/target/ruoyi-admin.jar"

if [[ "${BUILD:-0}" == "1" ]]; then
  (cd "$REPO_ROOT" && mvn -DskipTests package)
fi

if [[ ! -f "$JAR_PATH" ]]; then
  echo "backend jar not found. Run mvn -DskipTests package first: $JAR_PATH" >&2
  exit 1
fi

SSH=(ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10)
SCP=(scp -o StrictHostKeyChecking=no -o ConnectTimeout=30)
if command -v sshpass >/dev/null 2>&1; then
  SSH=(sshpass -p "$PASSWORD" "${SSH[@]}")
  SCP=(sshpass -p "$PASSWORD" "${SCP[@]}")
fi

TARGET="$SSH_USER@$SERVER_HOST"
"${SCP[@]}" "$JAR_PATH" "$TARGET:$REMOTE_PATH"
"${SSH[@]}" "$TARGET" "echo '$PASSWORD' | sudo -S systemctl restart enterprise-backend && systemctl is-active enterprise-backend"
echo "Enterprise backend deployed."
