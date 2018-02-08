#!/bin/bash
set -euo pipefail

ORCHESTRATOR_CONFIG_URL=$1
shift

./gradlew --no-daemon \
  :server:sonar-db-core:createDB \
  :server:sonar-db-core:test \
  :server:sonar-db-migration:test \
  :server:sonar-db-dao:test \
  -Dorchestrator.configUrl=$ORCHESTRATOR_CONFIG_URL \
  $*
