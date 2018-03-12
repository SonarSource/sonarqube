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

function installNode {
  set +u
  source ~/.nvm/nvm.sh && nvm install 8
  set -u
}

#
# Configure Maven settings and install some script utilities
#
function configureTravis {
  mkdir -p ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v47 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}
configureTravis

#
# Travis fails on timeout when build does not print logs
# during 10 minutes. This function aims to bypass this
# behavior when building the slow sonar-server sub-project.
#
function keep_alive() {
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
  installNode

  # Used by Next
  export INITIAL_VERSION=$(cat gradle.properties | grep version | awk -F= '{print $2}')

  # Fetch all commit history so that SonarQube has exact blame information
  # for issue auto-assignment
  # This command can fail with "fatal: --unshallow on a complete repository does not make sense"
  # if there are not enough commits in the Git repository (even if Travis executed git clone --depth 50).
  # For this reason errors are ignored with "|| true"
  git fetch --unshallow || true

  if [ "$TRAVIS_REPO_SLUG" == "SonarSource/sonarqube" ]; then
    # public repository
    ./gradlew build --no-daemon --console plain

  elif [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    echo 'Build and analyze master'
    ./gradlew --no-daemon --console plain \
        -DbuildNumber=$BUILD_NUMBER \
        build sonarqube artifactoryPublish -PjacocoEnabled=true -Prelease=true \
        -Dsonar.host.url=$SONAR_HOST_URL \
        -Dsonar.login=$SONAR_TOKEN \
        -Dsonar.projectVersion=$INITIAL_VERSION \
        -Dsonar.analysis.buildNumber=$BUILD_NUMBER \
        -Dsonar.analysis.pipeline=$BUILD_NUMBER \
        -Dsonar.analysis.sha1=$GIT_COMMIT \
        -Dsonar.analysis.repository=$TRAVIS_REPO_SLUG

  elif [[ "$TRAVIS_BRANCH" == "branch-"* ]] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    echo 'Build release branch'
    ./gradlew --no-daemon --console plain \
        -DbuildNumber=$BUILD_NUMBER \
        build sonarqube artifactoryPublish -PjacocoEnabled=true -Prelease=true \
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
    ./gradlew --no-daemon --console plain \
        -DbuildNumber=$BUILD_NUMBER \
        build sonarqube artifactoryPublish -PjacocoEnabled=true -Prelease=true \
        -Dsonar.host.url=$SONAR_HOST_URL \
        -Dsonar.login=$SONAR_TOKEN \
        -Dsonar.analysis.buildNumber=$BUILD_NUMBER \
        -Dsonar.analysis.pipeline=$BUILD_NUMBER \
        -Dsonar.analysis.sha1=$TRAVIS_PULL_REQUEST_SHA \
        -Dsonar.analysis.prNumber=$TRAVIS_PULL_REQUEST \
        -Dsonar.analysis.repository=$TRAVIS_REPO_SLUG \
        -Dsonar.pullrequest.key=$TRAVIS_PULL_REQUEST \
        -Dsonar.pullrequest.branch=$TRAVIS_PULL_REQUEST_BRANCH \
        -Dsonar.pullrequest.base=$TRAVIS_BRANCH \
        -Dsonar.pullrequest.provider=github \
        -Dsonar.pullrequest.github.repository=$TRAVIS_REPO_SLUG

  else
    echo 'Build feature branch or external pull request'
    ./gradlew  --no-daemon --console plain \
        -DbuildNumber=$BUILD_NUMBER -Prelease=true \
        build artifactoryPublish
  fi
  ;;

WEB_TESTS)
  installNode
  curl -o- -L https://yarnpkg.com/install.sh | bash
  export PATH=$HOME/.yarn/bin:$PATH
  cd server/sonar-web && yarn && yarn validate
  cd ../sonar-vsts && yarn && yarn validate
  ;;
  
*)
  echo "Unexpected TARGET value: $TARGET"
  exit 1
  ;;

esac

