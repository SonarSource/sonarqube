#!/bin/sh

# Shortcut to start server. It must be already built.

cd sonar-application/target/
unzip sonarqube-*.zip
cd sonarqube-*
bin/macosx-universal-64/sonar.sh start
sleep 1
tail -100f logs/sonar.log
