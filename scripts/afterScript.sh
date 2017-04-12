#!/bin/bash
set -euo pipefail

RED='\033[0;31m'
NC='\033[0m' # No Color

printf "${RED}disk size after build${NC}\n"
df -h
du -sh $HOME
du -sh $HOME/.m2/repository
du -sh $HOME/.sonar
du -sh $HOME/build
du -sh $HOME/jvm
du -sh $HOME/maven
du -sh $HOME/phantomjs