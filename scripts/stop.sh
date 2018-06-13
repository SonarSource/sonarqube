#!/usr/bin/env bash
###############################
# Shortcut to stop server. It must be already built.
###############################

set -euo pipefail

ROOT=$(pwd)
source "$ROOT/scripts/editions.sh"
if [ -r "$ROOT/private/scripts/editions.sh" ]; then
  source "$ROOT/private/scripts/editions.sh"
fi

if [[ "$OSTYPE" == "darwin"* ]]; then
  OS='macosx-universal-64'
else
  OS='linux-x86-64'
fi

stopAny() {
  for edition in $EDITIONS; do
      SONAR_SH="$(distributionDirOf "$edition")/sonarqube-*/bin/$OS/sonar.sh"
      if ls $SONAR_SH &> /dev/null; then
        echo "$(baseFileNameOf "$edition") is unpacked"
        sh $SONAR_SH stop
      fi
  done
}

# check the script was called to avoid execute when script is only sourced
script_name=$(basename "$0")
if [ "$script_name" = "stop.sh" ]; then
  stopAny
fi
