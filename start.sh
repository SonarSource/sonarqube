#!/bin/sh

if [[ "$OSTYPE" == "darwin"* ]]; then
  OS='macosx-universal-64'
else
  OS='linux-x86-64'
fi

if ! ls sonar-application/target/sonarqube-*.zip &> /dev/null; then
  echo 'Sources are not built'
  ./build.sh
fi

cd sonar-application/target/
if ! ls sonarqube-*/bin/$OS/sonar.sh &> /dev/null; then
  unzip sonarqube-*.zip
fi
cd sonarqube-*
bin/$OS/sonar.sh restart
sleep 1
tail -100f logs/sonar.log
