#!/bin/bash
set -euo pipefail

: "${SONAR_HOST_URL?}" "${SONAR_TOKEN?}"

export GIT_SHA1=${CIRRUS_CHANGE_IN_REPO?}
export GITHUB_BASE_BRANCH=${CIRRUS_BASE_BRANCH:-}
export GITHUB_BRANCH=${CIRRUS_BRANCH?}
export GITHUB_REPO=${CIRRUS_REPO_FULL_NAME?}
export BUILD_NUMBER=${CI_BUILD_NUMBER?}
export PULL_REQUEST=${CIRRUS_PR:-false}
export PULL_REQUEST_SHA=${CIRRUS_BASE_SHA:-}
export PIPELINE_ID=${CIRRUS_BUILD_ID?}

INITIAL_VERSION=$(grep version gradle.properties | awk -F= '{print $2}')
export INITIAL_VERSION

git fetch --unshallow || true

sonar_params=(
  -Dsonar.projectKey=sonarqube
  -Dsonar.host.url="$SONAR_HOST_URL"
  -Dsonar.token="$SONAR_TOKEN"
  -Dsonar.analysis.buildNumber="$BUILD_NUMBER"
  -Dsonar.analysis.pipeline="$PIPELINE_ID"
  -Dsonar.analysis.repository="$GITHUB_REPO"
  -Dsonar.analysis.sha1="$GIT_SHA1"
)
if [ "$PULL_REQUEST" != "false" ]; then
  echo '======= Analyze pull request'
else
  echo "======= Analyze $GITHUB_BRANCH"
  sonar_params+=(
    -Dsonar.projectVersion="$INITIAL_VERSION"
  )
  if [ "${GITHUB_BRANCH}" != "${CIRRUS_DEFAULT_BRANCH?}" ]; then
    sonar_params+=(
      -Dsonar.branch.name="$GITHUB_BRANCH"
    )
  fi
fi
./gradlew --warn --stacktrace --console plain --build-cache \
  -Dwebdriver.chrome.driver=/usr/bin/chromedriver \
  build jacocoTestReport yarn_validate-ci sonar "${sonar_params[@]}"
