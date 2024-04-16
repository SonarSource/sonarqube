#!/bin/bash
set -euo pipefail

source .cirrus/cirrus-env

: "${SONAR_HOST_URL?}" "${SONAR_TOKEN?}"

git fetch --unshallow || true
if [ -n "${GITHUB_BASE_BRANCH:-}" ]; then
  git fetch origin "${GITHUB_BASE_BRANCH}"
fi

./gradlew sonar \
  -DbuildNumber="$BUILD_NUMBER" \
  -Dsonar.projectKey=sonarqube \
  -Dsonar.host.url="$SONAR_HOST_URL" \
  -Dsonar.token="$SONAR_TOKEN" \
  -Dsonar.analysis.buildNumber="$BUILD_NUMBER" \
  -Dsonar.analysis.pipeline="$PIPELINE_ID" \
  -Dsonar.analysis.repository="$GITHUB_REPO" \
  -Dsonar.analysis.sha1="$GIT_SHA1" \
  -Dsonar.exclusions=**/design-system/src/theme/**
  --parallel --configure-on-demand --console plain -Pqa
