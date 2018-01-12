#!/bin/bash
###############################
# exposes library functions to modify properties in a property 
#
# TODO function append_property
#
###############################

set -euo pipefail

function cnt_lines {
  local FILE=$1
  wc -l < "$FILE"
}

function write_prop {
  local PROPERTY=$1
  local VALUE=$2
  local FILE=$3

  # uncomment below to help debug calls to set_property
  #echo "setting property $PROPERTY to value $VALUE in $FILE"
  
  echo >> "$FILE"
  echo "${PROPERTY}=${VALUE}" >> "$FILE"
}

function set_property {
  local PROPERTY=$1
  local VALUE=$2
  local FILE=$3

  local REGEXP="^${PROPERTY//\./\\.}[ \\t]*="

  if grep -q "$REGEXP" "$FILE"; then
     # delete line of specified property
    LINE_COUNT=$(cnt_lines "$FILE")

    if [[ "$OSTYPE" == "darwin"* ]]; then
      sed -i '' "/${REGEXP}/d" "$FILE"
    else
      sed -i "/${REGEXP}/d" "$FILE"
    fi

    # add property if at least one line deleted
    local NEW_LINE_COUNT=$(cnt_lines "$FILE")

    if [[ $LINE_COUNT -gt $NEW_LINE_COUNT ]]; then
      write_prop "$PROPERTY" "$VALUE" "$FILE"
    fi

  else
    write_prop "$PROPERTY" "$VALUE" "$FILE"
  fi
}
