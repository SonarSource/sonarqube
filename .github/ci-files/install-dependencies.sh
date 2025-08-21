#!/bin/bash

set -euo pipefail

sudo apt-get update && sudo apt-get install -y wget gpg ca-certificates

wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo apt-key add -
echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list

sudo mkdir -p /usr/share/man/man1
sudo apt-get update
sudo apt-get install -qqy temurin-8-jdk expect
sudo apt-get clean
sudo rm -rf /var/lib/apt/lists/*
