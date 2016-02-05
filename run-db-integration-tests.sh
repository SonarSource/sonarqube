#!/bin/bash
set -euo pipefail

ORCHESTRATOR_CONFIG_URL=$1
shift

cd it
mvn verify \
  -Dorchestrator.configUrl=$ORCHESTRATOR_CONFIG_URL \
  -Dwith-db-drivers \
  -Dcategory=Category1 \
  -Dsource.skip=true -B -e -V $*
