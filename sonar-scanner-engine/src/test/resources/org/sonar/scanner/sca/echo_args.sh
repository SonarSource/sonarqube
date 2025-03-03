#!/bin/bash

echo "Arguments Passed In:" $@

POSITIONAL_ARGS=()

while [[ $# -gt 0 ]]; do
  case $1 in
    --zip-filename)
      FILENAME="$2"
      shift
      shift
      ;;
    *)
      POSITIONAL_ARGS+=("$1")
      shift
      ;;
  esac
done

set -- "${POSITIONAL_ARGS[@]}" # restore positional parameters

# print specific env variables that should be defined here
echo "TIDELIFT_SKIP_UPDATE_CHECK=${TIDELIFT_SKIP_UPDATE_CHECK}"
echo "TIDELIFT_RECURSIVE_MANIFEST_SEARCH=${TIDELIFT_RECURSIVE_MANIFEST_SEARCH}"

# print filename location for debug purposes
echo "ZIP FILE LOCATION = ${FILENAME}"
echo "" > $FILENAME
