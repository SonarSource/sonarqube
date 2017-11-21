#!/usr/bin/env bash

set -euo pipefail

PATCHES_HOME=scripts/patches
USER_PATCHES_HOME=

if [ "${SONARQUBE_USER_PATCHES_HOME+x}" ]; then
    USER_PATCHES_HOME=$SONARQUBE_USER_PATCHES_HOME
fi

# $1: name(s) of patches to call, separated by comma(s)
# $2: path to SonarQube installation
call_patches() {
  local patches=$1
  local sq_home=$2
  local patch script
  local IFS=,
  
  for patch in $patches; do
    echo
    echo "******** $patch *******"

    if [ "$USER_PATCHES_HOME" -a -x "$USER_PATCHES_HOME/$patch.sh" ]; then
        script=$USER_PATCHES_HOME/$patch.sh
    elif [ -x "$PATCHES_HOME/$patch.sh" ]; then
        script=$PATCHES_HOME/$patch.sh
    elif [ "$USER_PATCHES_HOME" ]; then
        echo "Patch $patch is not an executable script in $PATCHES_HOME or $USER_PATCHES_HOME"
        return 1
    else
        echo "Patch $patch is not an executable script in $PATCHES_HOME"
        return 1
    fi
    "$script" "$sq_home"
  done

  echo
}

