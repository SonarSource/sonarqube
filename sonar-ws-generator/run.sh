#!/bin/bash

# Example:
# run.sh /path/to/orchestrator.properties

set -euo pipefail

mvn clean package -Prun-ws-generator
java -Dorchestrator.configUrl=file://$* -jar target/sonar-ws-generator-*-SNAPSHOT-jar-with-dependencies.jar
