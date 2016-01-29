#!/bin/bash

set -euo pipefail

function configureTravis {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v23 | tar zx --strip-components 1 -C ~/.local
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

    mvn deploy \
        -Pdeploy-sonarsource \
        -Dmaven.test.redirectTestOutputToFile=false \
        -B -e -V

  elif [ "$TRAVIS_PULL_REQUEST" != "false" ] && [ -n "${GITHUB_TOKEN-}" ]; then
    strongEcho 'Build and analyze pull request, no deploy'

    # No need for Maven phase "install" as the generated JAR file does not need to be installed
    # in Maven local repository. Phase "verify" is enough.

    mvn org.jacoco:jacoco-maven-plugin:prepare-agent verify sonar:sonar \
        -Pcoverage-per-test \
        -Dclirr=true \
        -Dmaven.test.redirectTestOutputToFile=false \
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

POSTGRES)
  psql -c 'create database sonar;' -U postgres

  runDatabaseCI "postgresql" "jdbc:postgresql://localhost/sonar" "postgres" ""
  ;;

MYSQL)
  mysql -e "CREATE DATABASE sonar CHARACTER SET UTF8;" -uroot
  mysql -e "CREATE USER 'sonar'@'localhost' IDENTIFIED BY 'sonar';" -uroot
  mysql -e "GRANT ALL ON sonar.* TO 'sonar'@'localhost';" -uroot
  mysql -e "FLUSH PRIVILEGES;" -uroot

  runDatabaseCI "mysql" "jdbc:mysql://localhost/sonar?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true&useConfigs=maxPerformance" "sonar" "sonar"
  ;;

WEB)
  set +eu
  source ~/.nvm/nvm.sh && nvm install 4
  npm install -g npm@3.5.2
  cd server/sonar-web && npm install && npm test
  ;;

IT)
  if [ "$IT_CATEGORY" == "Plugins" ] && [ ! -n "$GITHUB_TOKEN" ]; then
    echo "This job is ignored as it needs to access a private GitHub repository"
  else
    start_xvfb

    mvn install -Pit,dev -DskipTests -Dcategory=$IT_CATEGORY -Dmaven.test.redirectTestOutputToFile=false -e -Dsource.skip=true
  fi
  ;;

*)
  echo "Unexpected TARGET value: $TARGET"
  exit 1
  ;;

esac
