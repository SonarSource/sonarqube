#!/bin/bash
###############################
# usage: logs.sh [ -l ARG ] [ -n ARG ]
#  -l ARG: name of log file to display.
#          valid values are 'all', 'sonar', 'web', 'ce' and 'es' (case insensitive).
#          default value is 'all'.
#  -n ARG: number of log line to display. Value is passed to n option of tail.
#          default value is 25
###############################

set -euo pipefail

DEFAULT_LOG="all"
DEFAULT_LINES="25"
LOGS="sonar web ce es"

function toLower() {
  echo "$1" | tr '[:upper:]' '[:lower:]'
}

function checkLogArgument() {
  local logArg="$1"
  local lowerLogArg=$(toLower $logArg)

  if [ "$lowerLogArg" == "$DEFAULT_LOG" ]; then
    return
  fi

  for t in $LOGS; do
    if [ "$lowerLogArg" == "$t" ]; then
      return
    fi
  done

  echo "Unsupported -l argument $logArg"
  exit 1
}

function buildTailArgs() {
  local logArg=$(toLower $logArg)
  local logLines="$2"
  local res=""

  for t in $LOGS; do
    if [ "$logArg" == "$DEFAULT_LOG" ] || [ "$logArg" == "$t" ]; then
      res="$res -Fn $logLines $SQ_HOME/logs/$t.log"
    fi
  done

  echo "$res"
}

function doTail() {
  local logArg="$1"
  local logLines="${2:-"$DEFAULT_LINES"}"
  TAIL_ARG=$(buildTailArgs "$logArg" "$logLines")
  tail $TAIL_ARG
}

# check the script was called to avoid execute when script is only sourced
script_name=$(basename "$0")
if [ "$script_name" == "logs.sh" ]; then
  LOG="$DEFAULT_LOG"
  LINES="$DEFAULT_LINES"
  while getopts ":l:n:" opt; do
    case "$opt" in
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

  checkLogArgument "$LOG"

  ROOT=$(pwd)
  cd sonar-application/target/sonarqube-*
  SQ_HOME=$(pwd)
  cd "$ROOT"

  doTail "$LOG" "$LINES"
fi
