#!/bin/bash

set -euo pipefail

ORCHESTRATOR_CONFIG_URL=$1
shift

# install BOM
mvn -B install --non-recursive

# create DB
mvn -B generate-test-resources \
  -pl :sonar-db-core \
  -Dorchestrator.configUrl=$ORCHESTRATOR_CONFIG_URL \
  -Dwith-db-drivers

# execute tests
./gradlew --no-daemon \
  :server:sonar-db-core:test \
  :server:sonar-db-migration:test \
  :server:sonar-db-dao:test \
  -Dorchestrator.configUrl=$ORCHESTRATOR_CONFIG_URL
