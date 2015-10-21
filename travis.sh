#!/bin/bash

set -euo pipefail

function installTravisTools {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v19 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
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
set +eu
  source ~/.nvm/nvm.sh && nvm install 4
  cd server/sonar-web && npm install && npm test
  ;;

PRANALYSIS)
  export JAVA_HOME=/usr/lib/jvm/java-8-oracle
  export PATH=$JAVA_HOME/bin:$PATH
  java -Xmx32m -version
  javac -J-Xmx32m -version
  if [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
	  if [ -n "$SONAR_GITHUB_OAUTH" ]; then
	    echo "Start pullrequest analysis"
	    mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent verify sonar:sonar -B -e -V -Panalysis \
	     -Dmaven.test.failure.ignore=true \
	     -Dclirr=true \
	     -Dsonar.analysis.mode=preview \
	     -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
	     -Dsonar.github.repository=$SONAR_GITHUB_REPOSITORY \
	     -Dsonar.github.login=$SONAR_GITHUB_LOGIN \
	     -Dsonar.github.oauth=$SONAR_GITHUB_OAUTH \
	     -Dsonar.host.url=$SONAR_HOST_URL \
	     -Dsonar.login=$SONAR_LOGIN \
	     -Dsonar.password=$SONAR_PASSWD
	  else
	  	echo "No oauth token available"
	  fi
  else
  	echo "Not in a pull request"
  fi
  ;;

ITS)
  if [ "$IT_CATEGORY" == "plugins" ] && [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
    echo "Ignore this job since it needs access to private test licenses."
  else
    installTravisTools

    start_xvfb

    CATEGORIES=($(echo "$IT_CATEGORY" | tr '_' '\n'))
    CATEGORY1=${CATEGORIES[0]:-'NONE'}
    CATEGORY2=${CATEGORIES[1]:-'NONE'}

    mvn install -Pit,dev -DskipTests -Dcategory1="$CATEGORY1" -Dcategory2="$CATEGORY2" -Dmaven.test.redirectTestOutputToFile=false -e -Dsource.skip=true
  fi
  ;;

esac
