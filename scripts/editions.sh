#!/usr/bin/env bash

set -euo pipefail

DEFAULT_EDITION="community"
EDITIONS="community"

toLower() {
  echo "$1" | tr '[:upper:]' '[:lower:]'
}

checkEdition() {
  for t in $EDITIONS; do
    if [ "$1" = "$t" ]; then
      return
    fi
  done

  echo "Unsupported edition $1"
  exit 1
}


resolveAliases() {
  local lowerEditionAlias=$(toLower "$1")

  case "$lowerEditionAlias" in
    oss )
      echo community ;;
    * )
      echo "$lowerEditionAlias" ;;
  esac
}

distributionDirOf() {
  echo "sonar-application/build/distributions/"
}

baseFileNameOf() {
  echo "sonar-application"
}
