#!/bin/bash
set -euo pipefail

RED='\033[0;31m'
NC='\033[0m' # No Color

printf "${RED}disk size before build${NC}\n"
df -h
du -sh $HOME
#du -sh $HOME/.m2/repository
#du -sh $HOME/.sonar
#du -sh server/sonar-web/node
#du -sh server/sonar-web/node_modules
#du -sh $HOME/jvm
#du -sh $HOME/maven
#du -sh $HOME/phantomjs
ls -al $HOME

printf "${RED}create ramdisk mount point${NC}\n"
sudo mkdir -p /mnt/ramdisk
printf "${RED}create ramdisk${NC}\n"
sudo mount -t tmpfs -o size=5120m tmpfs /mnt/ramdisk
pushd /mnt/ramdisk
printf "${RED}move build and cached directories to ramdisk${NC}\n"
time sudo mv $HOME/build /mnt/ramdisk
time sudo mv $HOME/.m2 /mnt/ramdisk
time sudo mv $HOME/.sonar /mnt/ramdisk
time sudo mv $HOME/jvm /mnt/ramdisk
time sudo mv $HOME/maven /mnt/ramdisk
time sudo mv $HOME/phantomjs /mnt/ramdisk
printf "${RED}create links to ramdisked directories${NC}\n"
sudo ln -s /mnt/ramdisk/build $HOME/build
sudo ln -s /mnt/ramdisk/.m2 $HOME/.m2
sudo ln -s /mnt/ramdisk/.sonar $HOME/.sonar
sudo ln -s /mnt/ramdisk/jvm $HOME/jvm
sudo ln -s /mnt/ramdisk/maven $HOME/maven
sudo ln -s /mnt/ramdisk/phantomjs $HOME/phantomjs

printf "${RED}give permissions to travis on the ramdisk${NC}\n"
sudo chown -R travis:travis /mnt/ramdisk
sudo chown -R travis:travis $HOME
printf "${RED}File System after ramdisk setup:${NC}\n"
printf "${RED}home content${NC}\n"
ls -al $HOME
printf "${RED}ramdisk content${NC}\n"
ls -al /mnt/ramdisk
df -h
popd
printf "${RED}current dir content${NC}\n"
ls -al