#!/bin/bash

set -euo pipefail

function installTravisTools {
  curl -sSL https://raw.githubusercontent.com/sonarsource/travis-utils/v13/install.sh | bash
  source /tmp/travis-utils/env.sh
}

function prepareIts {
  installTravisTools
  travis_build "SonarSource/sonar-orchestrator" "0fe60edd0978300334ecc9101e4c10bcb05516d0"
  travis_start_xvfb
}

case "$JOB" in

H2)
  mvn verify -B -e -V
  ;;

POSTGRES)
  installTravisTools

  psql -c 'create database sonar;' -U postgres

  travis_runDatabaseCI "postgresql" "jdbc:postgresql://localhost/sonar" "postgres" ""
  ;;

MYSQL)
  installTravisTools

  mysql -e "CREATE DATABASE sonar CHARACTER SET UTF8;" -uroot
  mysql -e "CREATE USER 'sonar'@'localhost' IDENTIFIED BY 'sonar';" -uroot
  mysql -e "GRANT ALL ON sonar.* TO 'sonar'@'localhost';" -uroot
  mysql -e "FLUSH PRIVILEGES;" -uroot

  travis_runDatabaseCI "mysql" "jdbc:mysql://localhost/sonar?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true&useConfigs=maxPerformance" "sonar" "sonar"
  ;;

WEB)
  export DISPLAY=:99.0
  /sbin/start-stop-daemon --start --quiet --pidfile /tmp/custom_xvfb_99.pid --make-pidfile --background --exec /usr/bin/Xvfb -- :99 -ac -screen 0 1280x1024x16
  wget http://selenium-release.storage.googleapis.com/2.46/selenium-server-standalone-2.46.0.jar
  nohup java -jar selenium-server-standalone-2.46.0.jar &
  sleep 3
  cd server/sonar-web && npm install && npm test
  ;;

PRANALYSIS)
  if [ -n "$SONAR_GITHUB_OAUTH" ] && [ "${TRAVIS_PULL_REQUEST}" != "false" ]; then
    echo "Start pullrequest analysis"
    mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent verify sonar:sonar -B -e -V -Dmaven.test.failure.ignore=true -Dclirr=true \
     -Dsonar.analysis.mode=incremental \
     -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
     -Dsonar.github.repository=$SONAR_GITHUB_REPOSITORY \
     -Dsonar.github.login=$SONAR_GITHUB_LOGIN \
     -Dsonar.github.oauth=$SONAR_GITHUB_OAUTH \
     -Dsonar.host.url=$SONAR_HOST_URL \
     -Dsonar.login=$SONAR_LOGIN \
     -Dsonar.password=$SONAR_PASSWD
  fi
  ;;

ITS_QUALITYGATE)
  prepareIts
  mvn install -Pit,dev -DskipTests -Dsonar.runtimeVersion=DEV -Dcategory="qualitygate" -e
  ;;

ITS_ISSUE)
  prepareIts
  mvn install -Pit,dev -DskipTests -Dsonar.runtimeVersion=DEV -Dcategory="issue" -e
  ;;

ITS_UPDATECENTER)
  prepareIts
  mvn install -Pit,dev -DskipTests -Dsonar.runtimeVersion=DEV -Dcategory="updatecenter" -e
  ;;

ITS_TESTING)
  prepareIts
  mvn install -Pit,dev -DskipTests -Dsonar.runtimeVersion=DEV -Dcategory="testing" -e
  ;;

ITS_PLUGINS)
  if [ -n "$GITHUB_TOKEN" ]; then
    prepareIts
    mvn install -Pit,dev -DskipTests -Dsonar.runtimeVersion=DEV -Dcategory="plugins" -e
  fi
  ;;

*)
  echo "Invalid JOB choice [$JOB]"
  exit 1

esac
