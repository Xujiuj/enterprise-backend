#!/usr/bin/env sh
set -eu

APP_HOME="$(cd "$(dirname "$0")/../.." && pwd)"
JAR_PATH="${APP_HOME}/ruoyi-admin/target/ruoyi-admin.jar"
CONFIG_DIR="${APP_HOME}/config/"

exec java ${JAVA_OPTS:-} -jar "$JAR_PATH" \
  --spring.profiles.active=prod \
  --spring.config.additional-location="optional:file:${CONFIG_DIR}"
