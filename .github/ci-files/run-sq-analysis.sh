#!/bin/bash

ADDITIONAL_PARAMS="-Dsonar.host.url=https://next.sonarqube.com/sonarqube \
  -Dsonar.projectKey=org.sonarsource.sonarqube:sonarqube-private \
  -Dsonar.projectName=sonar-enterprise \
  -Dsonar.token=$SONARQUBE_NEXT_TOKEN \
  -Dsonar.analysis.buildNumber=$BUILD_NUMBER \
  -Dsonar.analysis.pipeline=$GITHUB_RUN_ID \
  -Dsonar.analysis.repository=$GITHUB_REPOSITORY \
  -Dsonar.analysis.sha1=$GITHUB_SHA \
  -Dsonar.sca.exclusions=.github/**,**/src/test/**,**/src/bbt/**,private/it-**/**,sonar-ws-generator/**,sonar-testing-ldap/**,sonar-testing-harness/**,sonar-scanner-engine/test-resources/** \
  "

# We need the full history
git fetch --unshallow || true

if [[ -n "${GITHUB_BASE_REF:-}" ]]; then
  echo "Fetching base branch: $GITHUB_BASE_REF"
  git fetch origin "${GITHUB_BASE_REF}"
fi

# buildNumber is required to properly set the sonar.buildString during the scanner run
./gradlew sonar \
  --parallel --configure-on-demand \
  -DbuildNumber=$BUILD_NUMBER \
  ${ADDITIONAL_PARAMS} \
  --console plain \
  -Pqa --profile
