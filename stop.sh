#!/bin/bash

# Shortcut to stop server. It must be already built.

if [[ "$OSTYPE" == "darwin"* ]]; then
  OS='macosx-universal-64'
else
  OS='linux-x86-64'
fi

sh sonar-application/target/sonarqube-*/bin/$OS/sonar.sh stop
