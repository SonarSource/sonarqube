#!/usr/bin/env bash
set -euo pipefail

./.travis/setup_ramdisk.sh

#
# Configure Maven settings and install some script utilities
#
configureTravis() {
  mkdir -p ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v62 | tar zx --strip-components 1 -C ~/.local
  # shellcheck disable=SC1090
  source ~/.local/bin/install
}
configureTravis

#
# Travis fails on timeout when build does not print logs
# during 10 minutes. This aims to bypass this
# behavior when building the slow sonar-server sub-project.
#
keep_alive() {
  while true; do
    echo -en "\a"
    sleep 60
  done
}
keep_alive &

# When a pull request is open on the branch, then the job related
# to the branch does not need to be executed and should be canceled.
# It does not book slaves for nothing.
# @TravisCI please provide the feature natively, like at AppVeyor or CircleCI ;-)
cancel_branch_build_with_pr || if [[ $? -eq 1 ]]; then exit 0; fi

# Used by Next
INITIAL_VERSION=$(grep version gradle.properties | awk -F= '{print $2}')
export INITIAL_VERSION

source ./.travis/setup_environment.sh

# Analyse SonarQube on NEXT
export SONAR_HOST_URL=https://next.sonarqube.com/sonarqube

# Fetch all commit history so that SonarQube has exact blame information
# for issue auto-assignment
# This command can fail with "fatal: --unshallow on a complete repository does not make sense"
# if there are not enough commits in the Git repository (even if Travis executed git clone --depth 50).
# For this reason errors are ignored with "|| true"
git fetch --unshallow || true

BUILD_START_DATETIME=$(date --utc +%FT%TZ)
echo "$BUILD_START_DATETIME" > /tmp/build_start_time
./gradlew build --warn --console plain

# exclude external pull requests
if [[ -n "${NEXT_TOKEN-}" ]]; then
  sonar_params=(-Dsonar.projectKey=sonarqube
    -Dsonar.host.url="$SONAR_HOST_URL"
    -Dsonar.token="$NEXT_TOKEN"
    -Dsonar.analysis.buildNumber="$BUILD_NUMBER"
    -Dsonar.analysis.pipeline="$BUILD_NUMBER"
    -Dsonar.analysis.sha1="$GIT_SHA1"
    -Dsonar.analysis.repository="$TRAVIS_REPO_SLUG")

  if [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
    echo '======= Analyze pull request'
    ./gradlew jacocoTestReport yarn_validate-ci sonarqube --warn --console plain \
      "${sonar_params[@]}" \
      -Dsonar.analysis.prNumber="$PULL_REQUEST_NUMBER" \
      -Dsonar.pullrequest.branch="$GIT_BRANCH" \
      -Dsonar.pullrequest.base="$PULL_REQUEST_BASE_BRANCH" \
      -Dsonar.pullrequest.key="$PULL_REQUEST_NUMBER"
  elif [ "${TRAVIS_BRANCH}" == "master" ]; then
    echo '======= Analyze master'
    ./gradlew jacocoTestReport yarn_validate-ci sonarqube --warn --console plain \
      "${sonar_params[@]}" \
      -Dsonar.projectVersion="$INITIAL_VERSION"
  else
    echo '======= Analyze branch'
    ./gradlew jacocoTestReport yarn_validate-ci sonarqube --warn --console plain \
      "${sonar_params[@]}" \
      -Dsonar.branch.name="$GIT_BRANCH" \
      -Dsonar.projectVersion="$INITIAL_VERSION"
  fi

  # Wait for 5mins, hopefully the report will be processed.
  sleep 5m
  ./.travis/run_iris.sh

  BUILD_END_DATETIME=$(date --utc +%FT%TZ)
  notify_burgr "build" "build" "$TRAVIS_JOB_WEB_URL" "$BUILD_START_DATETIME" "$BUILD_END_DATETIME" || true
fi
