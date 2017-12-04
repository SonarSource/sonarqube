#!/bin/bash

# Example:
# run.sh /path/to/orchestrator.properties

set -euo pipefail

mvn clean package -Prun-ws-generator
java -jar target/sonar-ws-generator-*-SNAPSHOT-jar-with-dependencies.jar
cp -R target/generated-sources/results/org/sonarqube/ws/client/* ../sonar-ws/src/main/java/org/sonarqube/ws/client
