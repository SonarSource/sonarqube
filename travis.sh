#!/usr/bin/env bash

echo "Start build"
echo "=============================="
./build.sh

echo "Done building"
ls -l **/build/**/*.jar
echo "=============================="

echo "Scan project"
echo "=============================="
wget https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-3.3.0.1492-linux.zip
unzip sonar-scanner-cli-3.3.0.1492-linux.zip
ls -laF
ls -laF sonar-scanner-3.3.0.1492-linux/
./sonar-scanner-3.3.0.1492-linux/bin/sonar-scanner
