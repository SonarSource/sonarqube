#!/bin/sh
#
# NOTE: sonar must be built
#
cd sonar-application/target/
unzip sonar-*.zip
cd sonar-*
bin/macosx-universal-64/sonar.sh console

