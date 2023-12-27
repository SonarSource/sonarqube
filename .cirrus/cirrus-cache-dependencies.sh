#!/bin/bash
set -euo pipefail

source .cirrus/cirrus-env

if [ -d "${GRADLE_USER_HOME?}/caches" ]; then
  echo "gradle dependency cache already exists, bypassing dependencies loading"
else
  ./gradlew cacheDependencies \
    --parallel --console plain --build-cache --info
  ./gradlew cacheDependencies \
    --parallel --console plain --build-cache -Pqa --info
fi
