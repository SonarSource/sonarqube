#!/bin/bash

set -euo pipefail

function configureTravis {
  mkdir -p ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v31 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}
configureTravis

function strongEcho {
  echo ""
  echo "================ $1 ================="
}

case "$TARGET" in

CI)
  if [ "${TRAVIS_BRANCH}" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    strongEcho 'Build and deploy'

    # Do not deploy a SNAPSHOT version but the release version related to this build
    set_maven_build_version $TRAVIS_BUILD_NUMBER

    # analysis is currently executed by SonarSource internal infrastructure
    mvn deploy \
        -Pdeploy-sonarsource \
        -B -e -V

  elif [[ "${TRAVIS_BRANCH}" == "branch-"* ]] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    strongEcho 'Build and deploy'

    # get current version from pom
    CURRENT_VERSION=`maven_expression "project.version"`

    if [[ $CURRENT_VERSION =~ "-SNAPSHOT" ]]; then
      echo "======= Found SNAPSHOT version ======="
      # Do not deploy a SNAPSHOT version but the release version related to this build
      set_maven_build_version $TRAVIS_BUILD_NUMBER
    else
      echo "======= Found RELEASE version ======="
    fi

    # analysis is currently executed by SonarSource internal infrastructure
    mvn deploy \
        -Pdeploy-sonarsource \
        -B -e -V



  elif [ "$TRAVIS_PULL_REQUEST" != "false" ] && [ -n "${GITHUB_TOKEN:-}" ]; then
    strongEcho 'Build and analyze pull request, no deploy'

    # No need for Maven phase "install" as the generated JAR file does not need to be installed
    # in Maven local repository. Phase "verify" is enough.

    export MAVEN_OPTS="-Xmx1G -Xms128m"
    mvn org.jacoco:jacoco-maven-plugin:prepare-agent verify sonar:sonar \
        -Dclirr=true \
        -Dsonar.analysis.mode=issues \
        -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
        -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
        -Dsonar.github.oauth=$GITHUB_TOKEN \
        -Dsonar.host.url=$SONAR_HOST_URL \
        -Dsonar.login=$SONAR_TOKEN \
        -B -e -V

  else
    strongEcho 'Build, no analysis, no deploy'

    # No need for Maven phase "install" as the generated JAR file does not need to be installed
    # in Maven local repository. Phase "verify" is enough.

    mvn verify \
        -Dmaven.test.redirectTestOutputToFile=false \
        -B -e -V
  fi
  ;;

WEB)
  set +eu
  source ~/.nvm/nvm.sh && nvm install 4
  npm install -g npm@3.5.2
  cd server/sonar-web && npm install && npm test
  ;;

IT)
  start_xvfb
  mvn install -DskipTests=true -Dsource.skip=true -Denforcer.skip=true -B -e -V
  ./run-integration-tests.sh "$IT_CATEGORY" "" -Dmaven.test.redirectTestOutputToFile=false -Dexclude-qa-tests=true
  ;;

*)
  echo "Unexpected TARGET value: $TARGET"
  exit 1
  ;;

esac
