#!/usr/bin/env bash
set -euo pipefail

./.travis/setup_ramdisk.sh

#
# Configure Maven settings and install some script utilities
#
configureTravis() {
  mkdir -p ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v55 | tar zx --strip-components 1 -C ~/.local
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

git fetch --unshallow
./gradlew build --no-daemon --console plain

# the '-' at the end is needed when using set -u (the 'nounset' flag)
# see https://stackoverflow.com/a/9824943/641955
if [[ -n "${SONAR_TOKEN-}" ]]; then
  if [[ "${TRAVIS_BRANCH}" == "master" ]]; then 
    ./gradlew jacocoTestReport :server:sonar-web:yarn_validate-ci sonarqube --info --no-daemon --console plain \
      -Dsonar.projectKey=org.sonarsource.sonarqube:sonarqube \
      -Dsonar.organization=sonarsource \
      -Dsonar.host.url=https://sonarcloud.io \
      -Dsonar.login="$SONAR_TOKEN"
  else
    ./gradlew jacocoTestReport :server:sonar-web:yarn_validate-ci sonarqube --info --no-daemon --console plain \
      -Dsonar.projectKey=org.sonarsource.sonarqube:sonarqube \
      -Dsonar.organization=sonarsource \
      -Dsonar.host.url=https://sonarcloud.io \
      -Dsonar.login="$SONAR_TOKEN" \
      -Dsonar.branch.name="$TRAVIS_BRANCH"
  fi
  
  # Wait for 5mins, hopefully the report will be processed.
  sleep 5m
  ./.travis/run_iris.sh
fi