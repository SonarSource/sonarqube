#!/usr/bin/env bash
set -euo pipefail

./.travis/setup_ramdisk.sh

#
# A (too) old version of JDK8 is installed by default on Travis.
# This method is preferred over Travis apt oracle-java8-installer because
# JDK is kept in cache. It does not need to be downloaded from Oracle
# at each build.
#
installJdk8() {
  # copied from https://github.com/SonarSource/travis-utils/blob/master/bin/installJDK8
  JDK_RELEASE=181
  echo "Setup JDK 1.8u$JDK_RELEASE"
  mkdir -p ~/jvm
  pushd ~/jvm > /dev/null

  if [ ! -d "jdk1.8.0_$JDK_RELEASE" ]; then
    {
      wget --quiet --continue --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u181-b13/96a7b8442fe848ef90c96a2fad6ed6d1/jdk-8u181-linux-x64.tar.gz
    } || {
      echo "failed to download JDK 1.8u$JDK_RELEASE"
      exit 1
    }
    tar xzf jdk-8u$JDK_RELEASE-linux-x64.tar.gz
    rm jdk-8u$JDK_RELEASE-linux-x64.tar.gz
  fi
  popd > /dev/null
  export JAVA_HOME=~/jvm/jdk1.8.0_$JDK_RELEASE
  export PATH=$JAVA_HOME/bin:$PATH
  echo "JDK 1.8u$JDK_RELEASE installed"
}

#
# Configure Maven settings and install some script utilities
#
configureTravis() {
  mkdir -p ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v50 | tar zx --strip-components 1 -C ~/.local
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

case "$TARGET" in

BUILD)
  installJdk8
  ./gradlew build sonarqube --no-daemon --console plain \
  -PjacocoEnabled=true \
  -Dsonar.projectKey=org.sonarsource.sonarqube:sonarqube \
  -Dsonar.organization=sonarsource \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.login=$SONAR_TOKEN
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

