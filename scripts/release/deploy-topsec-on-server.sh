#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

echo "Building customized distribution package"
"${ROOT_DIR}/scripts/release/build-topsec-distribution.sh"

echo "Fetching Chinese language plugin"
"${ROOT_DIR}/scripts/release/fetch-zh-plugin.sh"

echo "Starting Docker Compose stack with a fresh image build"
(
  cd "${ROOT_DIR}"
  docker compose up -d --build
)

echo "Deployment finished. Check runtime status with:"
echo "  docker compose ps"
echo "  docker compose logs -f app"
