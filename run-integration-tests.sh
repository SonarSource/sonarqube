#!/bin/bash
# Run integration tests. SonarQube must be already built in order to
# make the ZIP file available for tests.

# Arguments:
# 1. the category of tests. Possible values: "Category1", "Category2" ,"Category3", "Category4", "Category5" and "Plugins"
# 2. the path to Orchestrator properties file. If empty, then default values are used. Example: "file:///Users/me/orchestrator.properties"
#
# Examples:
# ./run-integration-tests.sh "Category1" "file:///Users/me/orchestrator-mysql56.properties"
# ./run-integration-tests.sh "Category1" ""

set -euo pipefail

CATEGORY=$1
ORCHESTRATOR_CONFIG_URL=$2
shift 2

cd tests/plugins
mvn clean package -B -e -V

cd ..
mvn verify \
  -Dcategory="$CATEGORY" \
  -Dorchestrator.configUrl=$ORCHESTRATOR_CONFIG_URL \
  -Pwith-db-drivers \
  -B -e -V $*
