#!/bin/bash
###############################
# usage: start.sh [ -e ARG ] [ -p ARG ] [ -l ARG ]
#  -e ARG: edition to run
#          valid values are 'oss' (Open Source), 'dev' (Developer), 'ent' (Enterprise) and 'dce' (Data Center) (case insensitive)
#          default value is 'oss'.
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

checkEditionArgument "$EDITION"
checkLogArgument "$LOG"

if [[ "${OSTYPE:-}" == "darwin"* ]]; then
  OS='macosx-universal-64'
else
  OS='linux-x86-64'
fi

OSS_ZIP="$(distributionDirOf "oss")/$(baseFileNameOf "oss")-*.zip"
if ! ls ${OSS_ZIP} &> /dev/null; then
  echo 'Sources are not built'
  "$ROOT"/build.sh
fi

# stop SQ running in any instance
stopAny

cd "$(distributionDirOf "$EDITION")"

TARGET_DIR="$(targetDirOf "$EDITION")"
SH_FILE="${TARGET_DIR}/sonarqube-*/bin/$OS/sonar.sh"
if ! ls ${SH_FILE} &> /dev/null; then
  echo "Unpacking ${TARGET_DIR}..."
  ZIP_FILE="$(baseFileNameOf "$EDITION")-*.zip"
  unzip -qq ${ZIP_FILE} -d "${TARGET_DIR}"
fi
cd $(find ${TARGET_DIR}/sonarqube-* -type d | head -1)

SQ_HOME=$(pwd)
cd "$ROOT"

source "$ROOT"/scripts/patches_utils.sh

SQ_EXEC="$SQ_HOME/bin/$OS/sonar.sh"

# invoke patches if at least one was specified
# each patch is passed the path to the SQ instance home directory as first and only argument
if [ "$PATCHES" ]; then
  call_patches "$PATCHES" "$SQ_HOME"
fi

"$SQ_EXEC" start
sleep 1
doTail "$LOG"

