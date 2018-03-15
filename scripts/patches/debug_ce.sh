#!/usr/bin/env bash

set -euo pipefail

source scripts/property_utils.sh

SQ_HOME=$1

port=5006
echo "enabling debug on compute engine, listening on port $port"
set_property sonar.ce.javaAdditionalOpts "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$port" "$SQ_HOME/conf/sonar.properties"
