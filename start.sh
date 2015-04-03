#!/bin/bash

if [[ "$OSTYPE" == "darwin"* ]]; then
  OS='macosx-universal-64'
else
  OS='linux-x86-64'
fi

ls sonar-application/target/sonarqube-*.zip 1> /dev/null 2>&1
if [ "$?" != "0" ]; then
  echo 'Sources are not built'
  ./build.sh
fi

cd sonar-application/target/
ls sonarqube-*/bin/$OS/sonar.sh 1> /dev/null 2>&1
if [ "$?" != "0" ]; then
  unzip sonarqube-*.zip
fi
cd sonarqube-*
bin/$OS/sonar.sh restart
sleep 1
tail -100f logs/sonar.log
