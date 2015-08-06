#!/bin/bash
###############################
# usage: start.sh [ -p ARG ]
#  -p ARG: name(s) of patch separated by colon (name of patch is filename without extension)
###############################

ROOT=$(pwd)

PATCHES=""
while getopts ":p:" opt; do
  case "$opt" in
    p) PATCHES=$OPTARG
       ;;
    \?)
      >&2 echo "Unsupported option $OPTARG"
      exit 1 
      ;;
  esac
done

if [[ "$OSTYPE" == "darwin"* ]]; then
  OS='macosx-universal-64'
else
  OS='linux-x86-64'
fi

ls sonar-application/target/sonarqube-*.zip 1> /dev/null 2>&1
if [ "$?" != "0" ]; then
  echo 'Sources are not built'
  ./build.sh
fi

cd sonar-application/target/
ls sonarqube-*/bin/$OS/sonar.sh 1> /dev/null 2>&1
if [ "$?" != "0" ]; then
  echo "Unzipping SQ..."
  unzip -qq sonarqube-*.zip
fi
cd sonarqube-*

# from that point on, strict bash
set -euo pipefail
SQ_HOME=$(pwd)
cd $ROOT

source $ROOT/scripts/patches_utils.sh

SQ_EXEC=$SQ_HOME/bin/$OS/sonar.sh

$SQ_EXEC stop

# invoke patches if at least one was specified
# each patch is passed the path to the SQ instance home directory as first and only argument
if [ "$PATCHES" != "" ]; then
  call_patches $PATCHES $SQ_HOME
fi

$SQ_EXEC start
sleep 1
tail -100f $SQ_HOME/logs/sonar.log
