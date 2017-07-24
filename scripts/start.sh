#!/bin/bash
###############################
# usage: start.sh [ -p ARG ] [ -l ARG ]
#  -p ARG: name(s) of patch separated by colon (name of patch is filename without extension)
#  -l ARG: name of log file to display.
#          valid values are 'all', 'sonar', 'web', 'ce' and 'es' (case insensitive).
#          default value is 'all'.
###############################

set -euo pipefail

ROOT=$(pwd)

source "$ROOT"/scripts/logs.sh

PATCHES=""
LOG="$DEFAULT_LOG"
while getopts ":p:l:" opt; do
  case "$opt" in
    p) PATCHES=$OPTARG
       ;;
    l) LOG=${OPTARG:=$DEFAULT_LOG}
       ;;
    \?)
      echo "Unsupported option $OPTARG" >&2
      exit 1 
      ;;
  esac
done

checkLogArgument "$LOG"

if [[ "${OSTYPE:-}" == "darwin"* ]]; then
  OS='macosx-universal-64'
else
  OS='linux-x86-64'
fi

if ! ls sonar-application/target/sonarqube-*.zip &> /dev/null; then
  echo 'Sources are not built'
  "$ROOT"/build.sh
fi

cd sonar-application/target/
if ! ls sonarqube-*/bin/$OS/sonar.sh &> /dev/null; then
  echo "Unzipping SQ..."
  unzip -qq sonarqube-*.zip
fi
cd $(find sonarqube-* -type d | head -1)

SQ_HOME=$(pwd)
cd "$ROOT"

source "$ROOT"/scripts/patches_utils.sh

SQ_EXEC=$SQ_HOME/bin/$OS/sonar.sh

"$SQ_EXEC" stop

# invoke patches if at least one was specified
# each patch is passed the path to the SQ instance home directory as first and only argument
if [ "$PATCHES" ]; then
  call_patches "$PATCHES" "$SQ_HOME"
fi

"$SQ_EXEC" start
sleep 1
doTail "$LOG"

