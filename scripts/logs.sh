#!/usr/bin/env bash
###############################
# usage: logs.sh [ -e ARG ] [ -l ARG ] [ -n ARG ]
#  -e ARG: edition to run
#          valid values are 'oss' (Open Source), 'dev' (Developer), 'ent' (Enterprise) and 'ha' (HA) (case insensitive)
#          default value is 'oss'.
#  -l ARG: name of log file to display.
#          valid values are 'all', 'sonar', 'web', 'ce' and 'es' (case insensitive).
#          default value is 'all'.
#  -n ARG: number of log line to display. Value is passed to n option of tail.
#          default value is 25
###############################

set -euo pipefail

ROOT="$(pwd)"
source "$ROOT/scripts/editions.sh"
if [ -r "$ROOT/private/scripts/editions.sh" ]; then
  source "$ROOT/private/scripts/editions.sh"
fi

DEFAULT_LOG="all"
DEFAULT_LINES="25"
LOGS="sonar web ce es"

toLower() {
  echo "$1" | tr '[:upper:]' '[:lower:]'
}

checkLogArgument() {
  local logArg="$1"
  local lowerLogArg=$(toLower $logArg)

  if [ "$lowerLogArg" = "$DEFAULT_LOG" ]; then
    return
  fi

  for t in $LOGS; do
    if [ "$lowerLogArg" = "$t" ]; then
      return
    fi
  done

  echo "Unsupported -l argument $logArg"
  exit 1
}

buildTailArgs() {
  local logArg="$(toLower $1)"
  local logLines="$2"
  local res=""

  for t in $LOGS; do
    if [ "$logArg" = "$DEFAULT_LOG" ] || [ "$logArg" = "$t" ]; then
      res="$res -Fn $logLines $SQ_HOME/logs/$t.log"
    fi
  done

  echo "$res"
}

doTail() {
  local logArg="$1"
  local logLines="${2:-"$DEFAULT_LINES"}"
  TAIL_ARG=$(buildTailArgs "$logArg" "$logLines")
  tail $TAIL_ARG
}

# check the script was called to avoid execute when script is only sourced
script_name=$(basename "$0")
if [ "$script_name" = "logs.sh" ]; then
  LOG="$DEFAULT_LOG"
  LINES="$DEFAULT_LINES"
  EDITION="$DEFAULT_EDITION"
  while getopts ":e:l:n:" opt; do
    case "$opt" in
      e) EDITION=${OPTARG:=$DEFAULT_EDITION}
         ;;
      l) LOG=${OPTARG:=$DEFAULT_LOG}
         ;;
      n) LINES=${OPTARG:=$DEFAULT_LINES}
         ;;
      \?)
        echo "Unsupported option $OPTARG" >&2
        exit 1
        ;;
    esac
  done

  checkEdition "$EDITION"
  checkLogArgument "$LOG"

  SQ_HOME_WILDCARD="$(distributionDirOf "$EDITION")/sonarqube-*"
  if ! ls ${SQ_HOME_WILDCARD} &> /dev/null; then
    echo "$(baseFileNameOf "$EDITION") is not unpacked"
    exit 1
  fi
  cd ${SQ_HOME_WILDCARD}
  SQ_HOME=$(pwd)
  cd "$ROOT"

  doTail "$LOG" "$LINES"
fi
