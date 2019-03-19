#!/bin/bash
set -euo pipefail

SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"

pushd "$SCRIPTPATH"/..
pwd
./gradlew :sonar-ws-generator:build :sonar-ws-generator:fatJar
popd
java -jar build/libs/sonar-ws-generator-*-SNAPSHOT-jar-with-dependencies.jar
cp -R build/generated-sources/results/org/sonarqube/ws/client/* ../sonar-ws/src/main/java/org/sonarqube/ws/client
