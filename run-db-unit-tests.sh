#!/bin/bash
set -euo pipefail

ORCHESTRATOR_CONFIG_URL=$1
shift

mvn verify \
  -pl :sonar-db-core,:sonar-db-migration,:sonar-db-dao \
  -Dorchestrator.configUrl=$ORCHESTRATOR_CONFIG_URL \
  -Dwith-db-drivers \
  -B -e -V $*
