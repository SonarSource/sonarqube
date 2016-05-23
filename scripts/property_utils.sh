#!/bin/bash
###############################
# exposes library functions to modify properties in a property 
#
# TODO function append_property
#
###############################

set -euo pipefail

function cnt_lines() {
  FILE=$1
  cat $1 | wc -l
}

function write_prop() {
  PROPERTY=$1
  VALUE=$2
  FILE=$3

  # uncomment below to help debug calls to set_property
  #echo "setting property $PROPERTY to value $VALUE in $FILE"
  
  echo "" >> $FILE
  echo "${PROPERTY}=${VALUE}" >> $FILE 
}

function set_property() {
  PROPERTY=$1
  VALUE=$2
  FILE=$3

  REGEXP="${PROPERTY//\./\\.}\\s*="

  if grep -q "$REGEXP" "$FILE"; then
     # delete line of specified property
    LINE_COUNT=$(cnt_lines $FILE)

    # Create new file and replace afterwards. Avoids corruption if update fails
    sed -e /${REGEXP}/d "$FILE" > "${FILE}.new" && mv "${FILE}.new" "${FILE}"

    # add property if at least one line deleted
    NEW_LINE_COUNT=$(cnt_lines $FILE)

    if [[ $LINE_COUNT -gt $NEW_LINE_COUNT ]]; then
      write_prop $1 $2 $3
    fi

  else
    write_prop $1 $2 $3
  fi
}


