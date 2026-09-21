#!/usr/bin/env bash
###############################
# usage: start.sh [ -e ARG ] [ -p ARG ] [ -l ARG ]
#  -e ARG: edition to run
#          valid values are (case insensitive):
#             'community', 'oss',
#             'developer', 'dev',
#             'enterprise', 'ent',
#             'datacenter', 'dc', 'dce'.
#          default value is 'community'.
#  -p ARG: name(s) of patch separated by colon (name of patch is filename without extension)
#  -l ARG: name of log file to display.
#          valid values are 'all', 'sonar', 'web', 'ce' and 'es' (case insensitive).
#          default value is 'all'.
###############################

set -euo pipefail

ROOT=$(pwd)

source "$ROOT/scripts/editions.sh"
if [ -r "$ROOT/private/scripts/editions.sh" ]; then
  source "$ROOT/private/scripts/editions.sh"
fi
source "$ROOT/scripts/logs.sh"
source "$ROOT/scripts/stop.sh"
source "$ROOT/scripts/os.sh"

PATCHES=""
EDITION="$DEFAULT_EDITION"
LOG="$DEFAULT_LOG"
while getopts ":e:p:l:" opt; do
  case "$opt" in
    e) EDITION=${OPTARG:=$DEFAULT_EDITION}
       ;;
    p) PATCHES="$OPTARG"
       ;;
    l) LOG=${OPTARG:=$DEFAULT_LOG}
       ;;
    \?)
      echo "Unsupported option $OPTARG" >&2
      exit 1 
      ;;
  esac
done

EDITION=$(resolveAliases "$EDITION")
checkEdition "$EDITION"
checkLogArgument "$LOG"

OSS_ZIP="$(distributionDirOf "community")/$(baseFileNameOf "community")-*.zip"
if ! ls ${OSS_ZIP} &> /dev/null; then
  echo 'Sources are not built'
  "$ROOT"/build.sh
fi

# stop SQ running in any instance
stopAny

cd "$(distributionDirOf "$EDITION")"

SH_FILE_DIR="sonarqube-*/bin/$OS_DIR/"
if ! ls $SH_FILE_DIR &> /dev/null; then
  BASE_FILE_NAME="$(baseFileNameOf "$EDITION")"
  echo "Unpacking ${BASE_FILE_NAME}..."
  ZIP_FILE="${BASE_FILE_NAME}-*.zip"
  unzip -qq ${ZIP_FILE}
fi
cd $(find sonarqube-* -type d | head -1)

SQ_HOME=$(pwd)
cd "$ROOT"

source "$ROOT"/scripts/patches_utils.sh

# Dev instances must not report to production telemetry. Unlike the integration-test instances, this one
# is not started through Orchestrator, so nothing redirects it to telemetry-staging on our behalf.
# Applied before patches so `-p telemetry` (scripts/patches/telemetry.sh) can still point it at your endpoint.
source "$ROOT"/scripts/property_utils.sh
echo "disabling telemetry (use '-p telemetry' to send to your own endpoint instead)"
set_property sonar.telemetry.enable false "$SQ_HOME/conf/sonar.properties"
# Even when disabled, the daemon sends a one-off opt-out request at startup; keep it local.
set_property sonar.telemetry.url http://localhost "$SQ_HOME/conf/sonar.properties"
set_property sonar.telemetry.metrics.url http://localhost "$SQ_HOME/conf/sonar.properties"
# Same for the gessie event bus.
set_property sonar.gessie.url http://localhost "$SQ_HOME/conf/sonar.properties"

SQ_EXEC="$SQ_HOME/bin/$OS_DIR/$SH_FILE"

# invoke patches if at least one was specified
# each patch is passed the path to the SQ instance home directory as first and only argument
if [ "$PATCHES" ]; then
  call_patches "$PATCHES" "$SQ_HOME"
fi

runSQ $SQ_EXEC
sleep 1
doTail "$LOG"
