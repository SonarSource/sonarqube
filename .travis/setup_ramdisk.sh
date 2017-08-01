#!/bin/bash
set -euo pipefail

RED='\033[0;31m'
NC='\033[0m' # No Color
printf "${RED}SETUP RAMDISK${NC}\n"
printf "${RED}disk size before build${NC}\n"
df -h
du -sh $HOME
du -sh $TRAVIS_BUILD_DIR

printf "${RED}move original TRAVIS_BUILD_DIR${NC}\n"
sudo mv $TRAVIS_BUILD_DIR $TRAVIS_BUILD_DIR.ori
printf "${RED}create ramdisk mount point${NC}\n"
sudo mkdir -p $TRAVIS_BUILD_DIR
printf "${RED}create ramdisk${NC}\n"
sudo mount -t tmpfs -o size=8192m tmps $TRAVIS_BUILD_DIR
printf "${RED}copy TRAVIS_BUILD_DIR to ramdisk${NC}\n"
time sudo cp -R $TRAVIS_BUILD_DIR.ori/. $TRAVIS_BUILD_DIR
printf "${RED}give permissions to travis on its TRAVIS_BUILD_DIR in ramdisk${NC}\n"
sudo chown -R travis:travis $TRAVIS_BUILD_DIR

