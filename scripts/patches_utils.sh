#!/bin/bash
###############################
# exposes library functions to modify properties in a property 
###############################

set -euo pipefail

PATCHES_HOME=scripts/patches

# $1: name(s) of patches to call, separated by a colon
# all other arguments are passed as is to the patches
function call_patches() {
  PATCHES=$1
  ARGS=${@:2}

  IFS=','
  for PATCH in $PATCHES; do
    #echo "calling $PATCHES_HOME/$PATCH.sh $ARGS"
    echo ""
    echo "******** $PATCH *******"
    $PATCHES_HOME/$PATCH.sh $ARGS
  done

  echo ""
}


