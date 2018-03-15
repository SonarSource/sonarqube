#!/usr/bin/env bash

set -euo pipefail

source scripts/property_utils.sh

SQ_HOME=$1

echo "configuring postgres"
set_property sonar.jdbc.url jdbc:postgresql://localhost:5432/sonarqube "$SQ_HOME/conf/sonar.properties"
set_property sonar.jdbc.username sonarqube "$SQ_HOME/conf/sonar.properties"
set_property sonar.jdbc.password sonarqube "$SQ_HOME/conf/sonar.properties"
