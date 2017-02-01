#!/bin/bash

# Shortcut to stop server. It must be already built.

if [[ "$OSTYPE" == "darwin"* ]]; then
  OS='macosx-universal-64'
else
  OS='linux-x86-64'
fi

SONAR_SH=sonar-application/target/sonarqube-*/bin/$OS/sonar.sh
if [ -f $SONAR_SH ]; then
  sh $SONAR_SH stop
fi
