#!/bin/bash
set -euo pipefail

ORCHESTRATOR_CONFIG_URL=$1
CATEGORY=$2
shift 2

cd it
mvn verify \
  -Dorchestrator.configUrl=$ORCHESTRATOR_CONFIG_URL \
  -Dcategory=$CATEGORY \
  -Dsource.skip=true -B -e -V $*
