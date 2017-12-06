#!/bin/bash
set -euo pipefail

SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"

cd $SCRIPTPATH
mvn clean package -Prun-ws-generator
java -jar target/sonar-ws-generator-*-SNAPSHOT-jar-with-dependencies.jar
cp -R target/generated-sources/results/org/sonarqube/ws/client/* ../sonar-ws/src/main/java/org/sonarqube/ws/client
