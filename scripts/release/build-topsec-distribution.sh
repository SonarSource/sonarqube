#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck disable=SC1091
source "${ROOT_DIR}/scripts/release/load-env.sh"

WEBAPP_DIR="${WEBAPP_DIR:-$(cd "${ROOT_DIR}/.." && pwd)/sonarqube-webapp}"
WEBAPP_BUILD_PATH="${WEBAPP_BUILD_PATH:-${WEBAPP_DIR}/apps/sq-server/build/webapp}"
GRADLE_ARGS="${GRADLE_ARGS:--x test}"
BUILD_STAGING_DIR="${ROOT_DIR}/build/docker"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-${ROOT_DIR}/build/.gradle-user-home}"
WRAPPER_PROPERTIES_FILE="${ROOT_DIR}/gradle/wrapper/gradle-wrapper.properties"

seed_gradle_wrapper_cache() {
  local distribution_name
  local source_dir
  local target_dir

  if [[ ! -f "${WRAPPER_PROPERTIES_FILE}" ]]; then
    return 0
  fi

  distribution_name="$(sed -n 's/^distributionUrl=.*\/\(gradle-[^/]*\)\.zip$/\1/p' "${WRAPPER_PROPERTIES_FILE}" | head -n 1)"
  if [[ -z "${distribution_name}" ]]; then
    return 0
  fi

  target_dir="${GRADLE_USER_HOME}/wrapper/dists/${distribution_name}"
  if find "${target_dir}" -type f -path '*/bin/gradle' -print -quit 2>/dev/null | grep -q .; then
    return 0
  fi

  source_dir="${HOME}/.gradle/wrapper/dists/${distribution_name}"
  if [[ ! -d "${source_dir}" ]]; then
    return 0
  fi

  echo "Seeding Gradle wrapper cache from ${source_dir}"
  mkdir -p "${target_dir}"
  cp -R "${source_dir}/." "${target_dir}/"
  find "${target_dir}" -type f \( -name '*.lck' -o -name '*.part' \) -delete || true
}

if [[ ! -d "${WEBAPP_DIR}" ]]; then
  echo "Webapp repository not found: ${WEBAPP_DIR}" >&2
  exit 1
fi

mkdir -p "${GRADLE_USER_HOME}" "${BUILD_STAGING_DIR}"
seed_gradle_wrapper_cache

echo "Installing webapp dependencies in ${WEBAPP_DIR}"
(
  cd "${WEBAPP_DIR}"
  corepack yarn install
  corepack yarn workspace sq-server build
)

if [[ ! -d "${WEBAPP_BUILD_PATH}" ]]; then
  echo "Expected webapp build output not found: ${WEBAPP_BUILD_PATH}" >&2
  exit 1
fi

echo "Building packaged server distribution"
(
  cd "${ROOT_DIR}"
  WEBAPP_BUILD_PATH="${WEBAPP_BUILD_PATH}" GRADLE_USER_HOME="${GRADLE_USER_HOME}" ./gradlew build ${GRADLE_ARGS}
)

DIST_ZIP="$(find "${ROOT_DIR}/sonar-application/build/distributions" -maxdepth 1 -type f -name '*.zip' | head -n 1)"
if [[ -z "${DIST_ZIP}" ]]; then
  echo "Unable to locate built distribution zip" >&2
  exit 1
fi

cp "${DIST_ZIP}" "${BUILD_STAGING_DIR}/sonarqube.zip"

echo "Staged distribution at ${BUILD_STAGING_DIR}/sonarqube.zip"
