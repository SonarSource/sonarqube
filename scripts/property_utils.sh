#!/bin/bash
###############################
# exposes library functions to modify properties in a property 
#
# TODO function append_property
#
###############################

set -euo pipefail

if [[ "$OSTYPE" == "darwin"* ]]; then
  SED_DISABLE_BACKUP=" ''"
else
  SED_DISABLE_BACKUP=""
fi

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

  REGEXP="${1//\./\\\.}\s*="
  if [ $(grep $REGEXP $FILE | wc -l) -eq 0 ]; then
    write_prop $1 $2 $3
  else
    # delete line of specified property
    LINE_COUNT=$(cnt_lines $FILE)
    sed -i $SED_DISABLE_BACKUP "/${REGEXP}/d" $FILE

    # add property if at least one line deleted
    NEW_LINE_COUNT=$(cnt_lines $FILE)
    if [ $LINE_COUNT -gt $NEW_LINE_COUNT ]; then
      write_prop $1 $2 $3
    fi
  fi
}


