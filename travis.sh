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

case "$TARGET" in

BUILD)
  installJdk8
  ./gradlew build --no-daemon --console plain
  ;;

WEB_TESTS)
  ./gradlew :server:sonar-web:yarn :server:sonar-web:yarn_validate --no-daemon --console plain
  ./gradlew :server:sonar-vsts:yarn :server:sonar-vsts:yarn_validate --no-daemon --console plain
  ;;
  
*)
  echo "Unexpected TARGET value: $TARGET"
  exit 1
  ;;

esac

