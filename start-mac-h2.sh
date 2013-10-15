#!/bin/sh

#
# NOTE: sonar must be built
#
cd sonar-application/target/
unzip sonarqube-*.zip
cd sonarqube-*
bin/macosx-universal-64/sonar.sh console
