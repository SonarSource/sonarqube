#!/bin/bash

# Example:
# run.sh /path/to/orchestrator.properties

set -euo pipefail

mvn clean package -Prun-ws-generator
java -Dorchestrator.configUrl=file://$* -jar target/sonar-ws-generator-*-SNAPSHOT-jar-with-dependencies.jar
cp -R target/generated-sources/results/org/sonarqube/ws/client/* ../sonar-ws-generated/src/main/java/org/sonarqube/ws/client
