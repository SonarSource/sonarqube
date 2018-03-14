#!/usr/bin/env bash

set -euo pipefail

source scripts/property_utils.sh

SQ_HOME=$1

port=5005
echo "enabling debug on web server, listening on port $port"
set_property sonar.web.javaAdditionalOpts "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$port" "$SQ_HOME/conf/sonar.properties"
