#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

IMAGE_NAMESPACE="${IMAGE_NAMESPACE:-${DOCKERHUB_NAMESPACE:?DOCKERHUB_NAMESPACE is required}}"
IMAGE_NAME="${IMAGE_NAME:-topsec-ai-code-audit}"
IMAGE_TAG="${IMAGE_TAG:-qianduanV1.0}"

"${ROOT_DIR}/scripts/release/build-topsec-image.sh"
docker push "${IMAGE_NAMESPACE}/${IMAGE_NAME}:${IMAGE_TAG}"
