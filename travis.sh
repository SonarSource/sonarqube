#!/bin/bash

set -euo pipefail

function installTravisTools {
  curl -sSL https://raw.githubusercontent.com/sonarsource/travis-utils/v4/install.sh | bash
}

function reset_ruby {
  unset GEM_PATH GEM_HOME RAILS_ENV
}

function install_jars {
  echo "Install jars into local maven repository"

  mkdir -p ~/.m2/repository
  cp -r /tmp/travis-utils/m2repo/* ~/.m2/repository
}

# Usage: runDatabaseCI "database" "jdbc_url" "login" "pwd"
function runDatabaseCI {
  # Build current version of SonarQube (Don't create a zip)
  mvn install -DskipTests -Pdev -Dassembly.format=dir -Dchecksum.failOnError=false -T2 -Dsource.skip=true

  # Start server
  reset_ruby
  cd sonar-application/target/sonarqube-*/sonarqube-*
  (exec java -jar lib/sonar-application-*.jar \
    -Dsonar.log.console=true \
    -Dsonar.jdbc.url=$2 -Dsonar.jdbc.username=$3 -Dsonar.jdbc.password=${4:-} \
    -Dsonar.web.javaAdditionalOpts="-Djava.security.egd=file:/dev/./urandom"
    "$@") &
  pid=$!

  # Wait for server to be up and running
  for i in {1..30}; do
    set +e
    curl -s http://localhost:9000/api/system/status | grep "UP"
    retval=$?
    set -e
    if [ $retval -eq 0 ]; then
      # Success. Let's stop the server
      # Should we use orchestrator's stop command?
      kill -9 $pid

      # Run the tests
      install_jars
      cd ../../../..
      mvn package -pl :sonar-db -am -PdbTests -Dsonar.jdbc.dialect=$1 -Dsonar.jdbc.url=$2 -Dsonar.jdbc.username=$3 -Dsonar.jdbc.password=${4:-} -V
      exit $?
    fi

    sleep 1
  done

  # Failed to start
  exit 1
}

case "$JOB" in

H2)
  mvn verify -B -e -V
  ;;

POSTGRES)
  installTravisTools

  psql -c 'create database sonar;' -U postgres

  runDatabaseCI "postgresql" "jdbc:postgresql://localhost/sonar" "postgres" ""
  ;;

MYSQL)
  installTravisTools

  mysql -e "CREATE DATABASE sonar CHARACTER SET UTF8;" -uroot
  mysql -e "CREATE USER 'sonar'@'localhost' IDENTIFIED BY 'sonar';" -uroot
  mysql -e "GRANT ALL ON sonar.* TO 'sonar'@'localhost';" -uroot
  mysql -e "FLUSH PRIVILEGES;" -uroot

  runDatabaseCI "mysql" "jdbc:mysql://localhost/sonar?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true&useConfigs=maxPerformance" "sonar" "sonar"
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
  if [ -n "$SONAR_GITHUB_OAUTH" ] && [ "${TRAVIS_PULL_REQUEST}" != "false" ]
  then
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
	installTravisTools

	travis_sonarqube_its "qualitygate"
  ;;

ITS_UPDATECENTER)
	installTravisTools

	travis_sonarqube_its "updatecenter"
  ;;

ITS_TESTING)
	installTravisTools

	travis_sonarqube_its "testing"
  ;;

*)
  echo "Invalid JOB choice [$JOB]"
  exit 1

esac
