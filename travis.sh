#!/bin/bash

set -euo pipefail

function installTravisTools {
  curl -sSL https://raw.githubusercontent.com/sonarsource/travis-utils/v2.1/install.sh | bash
}

case "$DATABASE" in

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

*)
  echo "Invalid DATABASE choice [$DATABASE]"
  exit 1

esac
