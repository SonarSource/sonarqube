#!/bin/bash

# Convert first argument to lowercase (e.g., Postgres -> postgres)
DB_CONFIG=${1,,}

set +e

exit_code=0

./gradlew :server:sonar-db-dao:createDB \
  --console plain --build-cache \
  -Pqa \
  -Dorchestrator.configUrl=file:///$GITHUB_WORKSPACE/.github/ci-files/config/orchestrator-$DB_CONFIG.properties || exit_code=1

./gradlew :server:sonar-db-dao:test \
  --console plain --build-cache \
  -Pqa \
  -Dorchestrator.configUrl=file:///$GITHUB_WORKSPACE/.github/ci-files/config/orchestrator-$DB_CONFIG.properties || exit_code=1

./gradlew :server:sonar-ce-task-projectanalysis:test \
  --console plain --build-cache \
  -Pqa \
  -Dorchestrator.configUrl=file:///$GITHUB_WORKSPACE/.github/ci-files/config/orchestrator-$DB_CONFIG.properties || exit_code=1

# SCA application contains mappers
./gradlew :private:core-extension-sca:application-tester:test \
  --console plain --build-cache \
  -Pqa \
  --offline \
  -Dorchestrator.configUrl=file:///$GITHUB_WORKSPACE/.github/ci-files/config/orchestrator-$DB_CONFIG.properties || exit_code=1

exit $exit_code
