#!/bin/bash
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "start the clock"
while [ ! -f stop ]; do
  #echo "File not found!"
  seconds=60; date1=$((`date +%s` + $seconds));
  while [ "$date1" -ge `date +%s` ]; do
    #echo -ne "$(date -u --date @$(($date1 - `date +%s` )) +%H:%M:%S)\r"; > /dev/null
    : #busy wait
  done
  printf "${RED}############# `date` ############${NC}\n"
done
