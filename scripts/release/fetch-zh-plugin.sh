#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

BUILD_DIR="${ROOT_DIR}/build/docker/plugins"
PLUGIN_TAG="${PLUGIN_TAG:-sonar-l10n-zh-plugin-26.4}"
PLUGIN_JAR_NAME="${PLUGIN_JAR_NAME:-${PLUGIN_TAG}.jar}"
PLUGIN_TARGET_NAME="${PLUGIN_TARGET_NAME:-sonar-l10n-zh-plugin.jar}"
PLUGIN_URL="${PLUGIN_URL:-https://github.com/xuhuisheng/sonar-l10n-zh/releases/download/${PLUGIN_TAG}/${PLUGIN_JAR_NAME}}"

mkdir -p "${BUILD_DIR}"

echo "Downloading language pack from: ${PLUGIN_URL}"
curl --fail --location --output "${BUILD_DIR}/${PLUGIN_TARGET_NAME}" "${PLUGIN_URL}"

echo "Saved plugin to ${BUILD_DIR}/${PLUGIN_TARGET_NAME}"
