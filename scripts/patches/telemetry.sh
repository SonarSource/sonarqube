#!/usr/bin/env bash
set -euo pipefail
source scripts/property_utils.sh
SQ_HOME=$1

echo "configuring telemetry"
set_property sonar.telemetry.url https://<your_account>.eu.ngrok.io "$SQ_HOME/conf/sonar.properties"
set_property sonar.telemetry.metrics.url https://<your_account>.eu.ngrok.io "$SQ_HOME/conf/sonar.properties"
set_property sonar.telemetry.enable true "$SQ_HOME/conf/sonar.properties"
set_property sonar.telemetry.compression false "$SQ_HOME/conf/sonar.properties"
set_property sonar.telemetry.frequencyInSeconds 10 "$SQ_HOME/conf/sonar.properties"
set_property sonar.telemetry.lock.delay 5 "$SQ_HOME/conf/sonar.properties"
