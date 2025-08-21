#!/bin/bash

./gradlew :server:sonar-db-dao:createDB \
  --console plain --build-cache \
  -Pqa \
  -Dorchestrator.configUrl=file:///$GITHUB_WORKSPACE/.github/ci-files/config/orchestrator-$1.properties

./gradlew :server:sonar-db-dao:test \
  --console plain --build-cache \
  -Pqa \
  -Dorchestrator.configUrl=file:///$GITHUB_WORKSPACE/.github/ci-files/config/orchestrator-$1.properties

./gradlew :server:sonar-ce-task-projectanalysis:test \
  --console plain --build-cache \
  -Pqa \
  -Dorchestrator.configUrl=file:///$GITHUB_WORKSPACE/.github/ci-files/config/orchestrator-$1.properties

# SCA application contains mappers
./gradlew :private:core-extension-sca:application-tester:test \
  --console plain --build-cache \
  -Pqa \
  --offline \
  -Dorchestrator.configUrl=file:///$GITHUB_WORKSPACE/.github/ci-files/config/orchestrator-$1.properties
