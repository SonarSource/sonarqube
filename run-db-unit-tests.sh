#!/bin/bash

set -euo pipefail

ORCHESTRATOR_CONFIG_URL=$1
shift

# create DB
mvn -B initialize \
  -pl :sonar-db-core \
  -Dorchestrator.configUrl=$ORCHESTRATOR_CONFIG_URL \
  -Dwith-db-drivers

# execute tests
./gradlew --no-daemon \
  :server:sonar-db-core:test \
  :server:sonar-db-migration:test \
  :server:sonar-db-dao:test \
  -Dorchestrator.configUrl=$ORCHESTRATOR_CONFIG_URL
