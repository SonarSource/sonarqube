#!/bin/bash
set -euo pipefail

function installPhantomJs {
  echo "Setup PhantomJS 2.1"
  mkdir -p ~/phantomjs
  pushd ~/phantomjs > /dev/null
  if [ ! -d "phantomjs-2.1.1-linux-x86_64" ]; then
    echo "Download PhantomJS"
    wget https://repox.sonarsource.com/public-3rd-parties/phantomjs/phantomjs-2.1.1-linux-x86_64.tar.bz2 -O phantomjs-2.1.1-linux-x86_64.tar.bz2
    tar -xf phantomjs-2.1.1-linux-x86_64.tar.bz2
    rm phantomjs-2.1.1-linux-x86_64.tar.bz2
  fi
  popd > /dev/null
  export PHANTOMJS_HOME=~/phantomjs/phantomjs-2.1.1-linux-x86_64
  export PATH=$PHANTOMJS_HOME/bin:$PATH
}

#
# A (too) old version of JDK8 is installed by default on Travis
#
function installJdk8 {
  echo "Setup JDK 1.8u92"
  mkdir -p ~/jvm
  pushd ~/jvm > /dev/null
  if [ ! -d "jdk1.8.0_92" ]; then
    echo "Download JDK8"
    wget -c --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u92-b14/jdk-8u92-linux-x64.tar.gz
    tar xzf jdk-8u92-linux-x64.tar.gz
    rm jdk-8u92-linux-x64.tar.gz
  fi
  popd > /dev/null
  export JAVA_HOME=~/jvm/jdk1.8.0_92
  export PATH=$JAVA_HOME/bin:$PATH
}

#
# Replaces the SNAPSHOT version by a version identifying the build.
#
# Exports the variables:
# - INITIAL_VERSION: version as defined in pom.xml
# - PROJECT_VERSION: build version. The name of this variable is important because
#   it's used by QA when extracting version from Artifactory build info.
#
# The build version is composed of 4 fields, including the semantic version and
# the build number provided by Travis.
#
# Example
# Before: 6.3-SNAPSHOT
# After:  6.3.0.12345
#
# Exception: GA release like "6.3" is kept as-is.
#
function fixBuildVersion {
  export INITIAL_VERSION=`maven_expression "project.version"`

  # remove suffix like -SNAPSHOT or -RC
  without_suffix=`echo $INITIAL_VERSION | sed "s/-.*//g"`

  # set the third field to '0' if missing, for example 6.3 becomes 6.3.0
  IFS=$'.'
  fields_count=`echo $without_suffix | wc -w`
  unset IFS
  if [ $fields_count -lt 3 ]; then
    without_suffix="$without_suffix.0"
  fi

  export PROJECT_VERSION="$without_suffix.$TRAVIS_BUILD_NUMBER"

  echo "Source Version: $INITIAL_VERSION"
  echo "Build Version : $PROJECT_VERSION"

  if [[ "$INITIAL_VERSION" == *"-"* ]]; then
    # SNAPSHOT or RC
    mvn org.codehaus.mojo:versions-maven-plugin:2.2:set -DnewVersion=$PROJECT_VERSION -DgenerateBackupPoms=false -B -e
  fi
}

#
# Configure Maven settings and install some script utilities
#
function configureTravis {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v33 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}
configureTravis

case "$TARGET" in

BUILD)

  # Hack to keep job alive even if no logs during more than 10 minutes.
  # That can occur when uploading sonarqube.zip to Artifactory.
  ./clock.sh &

  installJdk8
  fixBuildVersion

  # Minimal Maven settings
  export MAVEN_OPTS="-Xmx1G -Xms128m"
  MAVEN_ARGS="-Dmaven.test.redirectTestOutputToFile=false -Dsurefire.useFile=false -B -e -V -DbuildVersion=$PROJECT_VERSION"

  if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    echo 'Build and analyze master'

    # Fetch all commit history so that SonarQube has exact blame information
    # for issue auto-assignment
    # This command can fail with "fatal: --unshallow on a complete repository does not make sense"
    # if there are not enough commits in the Git repository (even if Travis executed git clone --depth 50).
    # For this reason errors are ignored with "|| true"
    git fetch --unshallow || true

    mvn org.jacoco:jacoco-maven-plugin:prepare-agent deploy sonar:sonar \
          $MAVEN_ARGS \
          -Pdeploy-sonarsource,release \
          -Dsonar.host.url=$SONAR_HOST_URL \
          -Dsonar.login=$SONAR_TOKEN \
          -Dsonar.projectVersion=$INITIAL_VERSION

  elif [[ "$TRAVIS_BRANCH" == "branch-"* ]] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    echo 'Build release branch'

    mvn deploy $MAVEN_ARGS -Pdeploy-sonarsource,release

  elif [ "$TRAVIS_PULL_REQUEST" != "false" ] && [ -n "${GITHUB_TOKEN:-}" ]; then
    echo 'Build and analyze internal pull request'

    mvn org.jacoco:jacoco-maven-plugin:prepare-agent deploy sonar:sonar \
        $MAVEN_ARGS \
        -Dsource.skip=true \
        -Pdeploy-sonarsource \
        -Dsonar.analysis.mode=preview \
        -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
        -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
        -Dsonar.github.oauth=$GITHUB_TOKEN \
        -Dsonar.host.url=$SONAR_HOST_URL \
        -Dsonar.login=$SONAR_TOKEN

  else
    echo 'Build feature branch or external pull request'

    mvn install $MAVEN_ARGS -Dsource.skip=true
  fi

  installPhantomJs
  ./run-integration-tests.sh "Lite" "" -Dorchestrator.browser=phantomjs
  ;;

WEB_TESTS)
  set +u
  source ~/.nvm/nvm.sh && nvm install 6
  curl -o- -L https://yarnpkg.com/install.sh | bash
  export PATH=$HOME/.yarn/bin:$PATH
  cd server/sonar-web && yarn && yarn test -- --runInBand
  ;;

*)
  echo "Unexpected TARGET value: $TARGET"
  exit 1
  ;;

esac

#stop the clock
touch stop
