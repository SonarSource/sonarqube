#!/bin/bash
set -euo pipefail

./.travis/setup_ramdisk.sh

#
# A (too) old version of JDK8 is installed by default on Travis.
# This method is preferred over Travis apt oracle-java8-installer because
# JDK is kept in cache. It does not need to be downloaded from Oracle
# at each build.
#
function installJdk8 {
  echo "Setup JDK 1.8u161"
  mkdir -p ~/jvm
  pushd ~/jvm > /dev/null
  if [ ! -d "jdk1.8.0_161" ]; then
    wget --quiet --continue --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u161-b12/2f38c3b165be4555a1fa6e98c45e0808/jdk-8u161-linux-x64.tar.gz
    tar xzf jdk-8u161-linux-x64.tar.gz
    rm jdk-8u161-linux-x64.tar.gz
  fi
  popd > /dev/null
  export JAVA_HOME=~/jvm/jdk1.8.0_161
  export PATH=$JAVA_HOME/bin:$PATH
}

#
# Maven 3.2.5 is installed by default on Travis. Maven 3.5 is preferred.
#
function installMaven {
  echo "Setup Maven"
  mkdir -p ~/maven
  pushd ~/maven > /dev/null
  if [ ! -d "apache-maven-3.5" ]; then
    echo "Download Maven 3.5"
    curl -sSL https://archive.apache.org/dist/maven/maven-3/3.5.0/binaries/apache-maven-3.5.0-bin.tar.gz | tar zx -C ~/maven
  fi
  popd > /dev/null
  export M2_HOME=~/maven/apache-maven-3.5.0
  export PATH=$M2_HOME/bin:$PATH
}

function installNode {
  set +u
  source ~/.nvm/nvm.sh && nvm install 8
  set -u
}

#
# Replaces the version defined in sources, usually x.y-SNAPSHOT,
# by a version identifying the build.
# The build version is composed of 4 fields, including the semantic version and
# the build number provided by Travis.
#
# Exported variables:
# - INITIAL_VERSION: version as defined in pom.xml
# - BUILD_VERSION: version including the build number
# - PROJECT_VERSION: target Maven version. The name of this variable is important because
#   it's used by QA when extracting version from Artifactory build info.
#
# Example of SNAPSHOT
# INITIAL_VERSION=6.3-SNAPSHOT
# BUILD_VERSION=6.3.0.12345
# PROJECT_VERSION=6.3.0.12345
#
# Example of RC
# INITIAL_VERSION=6.3-RC1
# BUILD_VERSION=6.3.0.12345
# PROJECT_VERSION=6.3-RC1
#
# Example of GA
# INITIAL_VERSION=6.3
# BUILD_VERSION=6.3.0.12345
# PROJECT_VERSION=6.3
#
function fixBuildVersion {
  export INITIAL_VERSION=`maven_expression "project.version"`

  # remove suffix -SNAPSHOT or -RC
  without_suffix=`echo $INITIAL_VERSION | sed "s/-.*//g"`

  IFS=$'.'
  fields_count=`echo $without_suffix | wc -w`
  unset IFS
  if [ $fields_count -lt 3 ]; then
    export BUILD_VERSION="$without_suffix.0.$TRAVIS_BUILD_NUMBER"
  else
    export BUILD_VERSION="$without_suffix.$TRAVIS_BUILD_NUMBER"
  fi

  if [[ "${INITIAL_VERSION}" == *"-SNAPSHOT" ]]; then
    # SNAPSHOT
    export PROJECT_VERSION=$BUILD_VERSION
    mvn org.codehaus.mojo:versions-maven-plugin:2.2:set -DnewVersion=$PROJECT_VERSION -DgenerateBackupPoms=false -B -e
  else
    # not a SNAPSHOT: milestone, RC or GA
    export PROJECT_VERSION=$INITIAL_VERSION
  fi

  echo "Build Version  : $BUILD_VERSION"
  echo "Project Version: $PROJECT_VERSION"
}

#
# Configure Maven settings and install some script utilities
#
function configureTravis {
  mkdir -p ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v41 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}
configureTravis

# When a pull request is open on the branch, then the job related
# to the branch does not need to be executed and should be canceled.
# It does not book slaves for nothing.
# @TravisCI please provide the feature natively, like at AppVeyor or CircleCI ;-)
cancel_branch_build_with_pr || if [[ $? -eq 1 ]]; then exit 0; fi

# configure environment variables for Artifactory
export GIT_COMMIT=$TRAVIS_COMMIT
export BUILD_NUMBER=$TRAVIS_BUILD_NUMBER
if [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
  export GIT_BRANCH=$TRAVIS_BRANCH
  unset PULL_REQUEST_BRANCH_TARGET
  unset PULL_REQUEST_NUMBER
else
  export GIT_BRANCH=$TRAVIS_PULL_REQUEST_BRANCH
  export PULL_REQUEST_BRANCH_TARGET=$TRAVIS_BRANCH
  export PULL_REQUEST_NUMBER=$TRAVIS_PULL_REQUEST
fi

case "$TARGET" in

BUILD)

  installJdk8
  installMaven
  installNode
  fixBuildVersion

  # Minimal Maven settings
  export MAVEN_OPTS="-Xmx1G -Xms128m"
  MAVEN_ARGS="-T 1C -Dmaven.test.redirectTestOutputToFile=false -Dsurefire.useFile=false -B -e -V -DbuildVersion=$BUILD_VERSION -Dtests.es.logger.level=WARN -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"


  # Fetch all commit history so that SonarQube has exact blame information
  # for issue auto-assignment
  # This command can fail with "fatal: --unshallow on a complete repository does not make sense"
  # if there are not enough commits in the Git repository (even if Travis executed git clone --depth 50).
  # For this reason errors are ignored with "|| true"
  git fetch --unshallow || true


  if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    echo 'Build and analyze master'
    mvn org.jacoco:jacoco-maven-plugin:prepare-agent deploy \
          $MAVEN_ARGS \
          -Pdeploy-sonarsource,release
        
    mvn sonar:sonar \
          -Dsonar.host.url=$SONAR_HOST_URL \
          -Dsonar.login=$SONAR_TOKEN \
          -Dsonar.projectVersion=$INITIAL_VERSION \
          -Dsonar.analysis.buildNumber=$BUILD_NUMBER \
          -Dsonar.analysis.pipeline=$BUILD_NUMBER \
          -Dsonar.analysis.sha1=$GIT_COMMIT \
          -Dsonar.analysis.repository=$TRAVIS_REPO_SLUG

  elif [[ "$TRAVIS_BRANCH" == "branch-"* ]] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    echo 'Build release branch'

    mvn org.jacoco:jacoco-maven-plugin:prepare-agent deploy \
        $MAVEN_ARGS \
        -Pdeploy-sonarsource,release

    mvn sonar:sonar \
        -Dsonar.host.url=$SONAR_HOST_URL \
        -Dsonar.login=$SONAR_TOKEN \
        -Dsonar.branch.name=$TRAVIS_BRANCH \
        -Dsonar.projectVersion=$INITIAL_VERSION \
        -Dsonar.analysis.buildNumber=$BUILD_NUMBER \
        -Dsonar.analysis.pipeline=$BUILD_NUMBER \
        -Dsonar.analysis.sha1=$GIT_COMMIT \
        -Dsonar.analysis.repository=$TRAVIS_REPO_SLUG
  
  elif [ "$TRAVIS_PULL_REQUEST" != "false" ] && [ -n "${GITHUB_TOKEN:-}" ]; then
    echo 'Build and analyze internal pull request'

    mvn org.jacoco:jacoco-maven-plugin:prepare-agent deploy \
        $MAVEN_ARGS \
        -Dsource.skip=true \
        -Pdeploy-sonarsource

    # TODO remove the sonar.pullrequest.github.* settings after sonar-core-plugins 7.1.0.330 is deployed on Next
    mvn sonar:sonar \
        -Dsonar.host.url=$SONAR_HOST_URL \
        -Dsonar.login=$SONAR_TOKEN \
        -Dsonar.branch.name=$TRAVIS_PULL_REQUEST_BRANCH \
        -Dsonar.branch.target=$TRAVIS_BRANCH \
        -Dsonar.analysis.buildNumber=$BUILD_NUMBER \
        -Dsonar.analysis.pipeline=$BUILD_NUMBER \
        -Dsonar.analysis.sha1=$TRAVIS_PULL_REQUEST_SHA \
        -Dsonar.analysis.prNumber=$TRAVIS_PULL_REQUEST \
        -Dsonar.analysis.repository=$TRAVIS_REPO_SLUG \
        -Dsonar.pullrequest.id=$TRAVIS_PULL_REQUEST \
        -Dsonar.pullrequest.github.id=$TRAVIS_PULL_REQUEST \
        -Dsonar.pullrequest.github.repository=$TRAVIS_REPO_SLUG

  else
    echo 'Build feature branch or external pull request'
    mvn deploy $MAVEN_ARGS -Pdeploy-sonarsource,release
  fi

  ./run-integration-tests.sh "Lite" ""
  ;;

WEB_TESTS)
  installNode
  curl -o- -L https://yarnpkg.com/install.sh | bash
  export PATH=$HOME/.yarn/bin:$PATH
  cd server/sonar-web && yarn && yarn validate
  ;;

*)
  echo "Unexpected TARGET value: $TARGET"
  exit 1
  ;;

esac
