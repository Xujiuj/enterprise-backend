#!/usr/bin/env sh
set -eu

APP_HOME="$(cd "$(dirname "$0")/../.." && pwd)"
JAR_PATH="${APP_HOME}/ruoyi-admin/target/ruoyi-admin.jar"

exec java -jar "$JAR_PATH" --enterprise-setup "$@"
