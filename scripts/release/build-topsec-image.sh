#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

IMAGE_NAMESPACE="${IMAGE_NAMESPACE:-${DOCKERHUB_NAMESPACE:-local}}"
IMAGE_NAME="${IMAGE_NAME:-topsec-ai-code-audit}"
IMAGE_TAG="${IMAGE_TAG:-qianduanV1.0}"

"${ROOT_DIR}/scripts/release/build-topsec-distribution.sh"
"${ROOT_DIR}/scripts/release/fetch-zh-plugin.sh"

docker build \
  --file "${ROOT_DIR}/Dockerfile" \
  --tag "${IMAGE_NAMESPACE}/${IMAGE_NAME}:${IMAGE_TAG}" \
  "${ROOT_DIR}"
