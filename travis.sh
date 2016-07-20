#!/bin/bash

set -euo pipefail

function configureTravis {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v31 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}
configureTravis
. installJDK8

case "$TARGET" in

CI)
  export MAVEN_OPTS="-Xmx1G -Xms128m"
  INITIAL_VERSION=`maven_expression "project.version"`

  if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    echo 'Analyse and trigger QA of master branch'

    # Fetch all commit history so that SonarQube has exact blame information
    # for issue auto-assignment
    # This command can fail with "fatal: --unshallow on a complete repository does not make sense" 
    # if there are not enough commits in the Git repository (even if Travis executed git clone --depth 50).
    # For this reason errors are ignored with "|| true"
    git fetch --unshallow || true
  
    # Do not deploy a SNAPSHOT version but the release version related to this build
    set_maven_build_version $TRAVIS_BUILD_NUMBER

    mvn org.jacoco:jacoco-maven-plugin:prepare-agent deploy sonar:sonar \
          -Pdeploy-sonarsource \
          -Dmaven.test.redirectTestOutputToFile=false \
          -Dsonar.host.url=$SONAR_HOST_URL \
          -Dsonar.login=$SONAR_TOKEN \
          -Dsonar.projectVersion=$INITIAL_VERSION \
          -B -e -V

  elif [[ "$TRAVIS_BRANCH" == "branch-"* ]] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    echo 'release branch: trigger QA, no analysis'

    if [[ $INITIAL_VERSION =~ "-SNAPSHOT" ]]; then
      echo "======= Found SNAPSHOT version ======="
      # Do not deploy a SNAPSHOT version but the release version related to this build
      set_maven_build_version $TRAVIS_BUILD_NUMBER
    else
      echo "======= Found RELEASE version ======="
    fi

    mvn deploy \
        -Pdeploy-sonarsource \
        -Dmaven.test.redirectTestOutputToFile=false \
        -B -e -V

  elif [ "$TRAVIS_PULL_REQUEST" != "false" ] && [ -n "${GITHUB_TOKEN:-}" ]; then
    echo 'Internal pull request: trigger QA and analysis'

    set_maven_build_version $TRAVIS_BUILD_NUMBER

    mvn org.jacoco:jacoco-maven-plugin:prepare-agent deploy sonar:sonar \
        -Pdeploy-sonarsource \
        -Dmaven.test.redirectTestOutputToFile=false \
        -Dsonar.analysis.mode=issues \
        -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
        -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
        -Dsonar.github.oauth=$GITHUB_TOKEN \
        -Dsonar.host.url=$SONAR_HOST_URL \
        -Dsonar.login=$SONAR_TOKEN \
        -B -e -V

  else
    echo 'Feature branch or external pull request: no QA, no analysis'

    # No need for Maven phase "install" as the generated JAR file does not need to be installed
    # in Maven local repository. Phase "verify" is enough.
    mvn verify \
        -Dmaven.test.redirectTestOutputToFile=false \
        -B -e -V
  fi

  ./run-integration-tests.sh "Lite" ""
  ;;

WEB)
  set +eu
  source ~/.nvm/nvm.sh && nvm install 4
  npm install -g npm@3.5.2
  cd server/sonar-web && npm install && npm test
  ;;

*)
  echo "Unexpected TARGET value: $TARGET"
  exit 1
  ;;

esac
