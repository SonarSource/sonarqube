#!/bin/bash

set -euo pipefail

DEFAULT_EDITION="oss"
EDITIONS="oss"

function toLower() {
  echo "$1" | tr '[:upper:]' '[:lower:]'
}

function checkEditionArgument() {
  local editionArg="$1"
  local lowerEditionArg=$(toLower $editionArg)

  if [ "$lowerEditionArg" == "$DEFAULT_EDITION" ]; then
    return
  fi

  for t in $EDITIONS; do
    if [ "$lowerEditionArg" == "$t" ]; then
      return
    fi
  done

  echo "Unsupported edition $editionArg"
  exit 1
}

function distributionDirOf() {
  local edition="$1"

  if [ "$edition" = "oss" ]; then
    echo "sonar-application/build/distributions/"
  else
    echo "unsupported edition $edition"
    exit 1
  fi
}

function baseFileNameOf() {
  local edition="$1"

  if [ "$edition" = "oss" ]; then
    echo "sonar-application"
  else
    echo "unsupported edition $edition"
    exit 1
  fi
}

function targetDirOf() {
  local edition="$1"

  if [ "$edition" = "oss" ]; then
    echo "sonarqube-oss"
  else
    echo "unsupported edition $edition"
    exit 1
  fi
}
