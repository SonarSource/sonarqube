#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

WEBAPP_DIR="${WEBAPP_DIR:-$(cd "${ROOT_DIR}/.." && pwd)/sonarqube-webapp}"
WEBAPP_BUILD_PATH="${WEBAPP_BUILD_PATH:-${WEBAPP_DIR}/apps/sq-server/build/webapp}"
GRADLE_ARGS="${GRADLE_ARGS:--x test}"
BUILD_STAGING_DIR="${ROOT_DIR}/build/docker"

if [[ ! -d "${WEBAPP_DIR}" ]]; then
  echo "Webapp repository not found: ${WEBAPP_DIR}" >&2
  exit 1
fi

echo "Installing webapp dependencies in ${WEBAPP_DIR}"
(
  cd "${WEBAPP_DIR}"
  corepack yarn install --immutable
  corepack yarn workspace sq-server build
)

if [[ ! -d "${WEBAPP_BUILD_PATH}" ]]; then
  echo "Expected webapp build output not found: ${WEBAPP_BUILD_PATH}" >&2
  exit 1
fi

echo "Building packaged server distribution"
(
  cd "${ROOT_DIR}"
  WEBAPP_BUILD_PATH="${WEBAPP_BUILD_PATH}" ./gradlew build ${GRADLE_ARGS}
)

DIST_ZIP="$(find "${ROOT_DIR}/sonar-application/build/distributions" -maxdepth 1 -type f -name '*.zip' | head -n 1)"
if [[ -z "${DIST_ZIP}" ]]; then
  echo "Unable to locate built distribution zip" >&2
  exit 1
fi

mkdir -p "${BUILD_STAGING_DIR}"
cp "${DIST_ZIP}" "${BUILD_STAGING_DIR}/sonarqube.zip"

echo "Staged distribution at ${BUILD_STAGING_DIR}/sonarqube.zip"
