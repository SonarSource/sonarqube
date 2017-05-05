#!/bin/bash
set -euo pipefail

RED='\033[0;31m'
NC='\033[0m' # No Color
printf "${RED}SETUP RAMDISK${NC}\n"
printf "${RED}disk size before build${NC}\n"
df -h
du -sh $HOME

printf "${RED}move original home${NC}\n"
sudo mv /home/travis /home/travis.ori
printf "${RED}create ramdisk mount point${NC}\n"
sudo mkdir -p /home/travis
printf "${RED}create ramdisk${NC}\n"
sudo mount -t tmpfs -o size=8192m tmps /home/travis
printf "${RED}copy home to ramdisk${NC}\n"
time sudo cp -R /home/travis.ori/. /home/travis
printf "${RED}give permissions to travis on its home in ramdisk${NC}\n"
sudo chown -R travis:travis /home/travis

