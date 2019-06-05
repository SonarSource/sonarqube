#!/usr/bin/env bash
###############################
# Helper for OS folders and scripts
###############################

set -euo pipefail

if [[ "${OSTYPE:-}" == "darwin"* ]]; then
  OS_DIR='macosx-universal-64'
  SH_FILE="sonar.sh"
elif [[ "${OSTYPE:-}" == "msys"* ]]; then
  OS_DIR='windows-x86-64'
  SH_FILE="StartSonar.bat"
else
  OS_DIR='linux-x86-64'
  SH_FILE="sonar.sh"
fi

runSQ() {
  if [[ "${OSTYPE:-}" == "msys"* ]]; then
    "$1"
  else
    "$1" start
  fi
}