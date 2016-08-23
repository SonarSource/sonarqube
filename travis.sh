#!/bin/bash

set -euo pipefail

function configureTravis {
  mkdir ~/.local
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
    # Do not deploy a SNAPSHOT version but the release version related to this build
    set_maven_build_version $TRAVIS_BUILD_NUMBER

    # analysis is currently executed by SonarSource internal infrastructure
    mvn deploy \
        -Pdeploy-sonarsource \
        -B -e -V

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
