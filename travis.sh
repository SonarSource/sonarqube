#!/bin/bash

set -euo pipefail

function installTravisTools {
  curl -sSL https://raw.githubusercontent.com/dgageot/travis-utils/master/install.sh | sh
	source /tmp/travis-utils/utils.sh
}

# Which build do we start?
if [ "$DATABASE" == "H2" ]; then
	mvn verify -B -e -V
elif [ "$DATABASE" == "POSTGRES" ]; then
  installTravisTools

	psql -c 'create database sonar;' -U postgres

  runDatabaseCI "postgresql" "jdbc:postgresql://localhost/sonar" "postgres" ''
elif [ "$DATABASE" == "MYSQL" ]; then
  installTravisTools

	mysql -e "CREATE DATABASE sonar CHARACTER SET UTF8;" -uroot
	mysql -e "CREATE USER 'sonar'@'localhost' IDENTIFIED BY 'sonar';" -uroot
	mysql -e "GRANT ALL ON sonar.* TO 'sonar'@'localhost';" -uroot
	mysql -e "FLUSH PRIVILEGES;" -uroot

  runDatabaseCI "mysql" "jdbc:mysql://localhost/sonar?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true&useConfigs=maxPerformance" "sonar" 'sonar'
fi
