#!/bin/sh

if [[ "$OSTYPE" == "darwin"* ]]; then
  OS='macosx-universal-64'
else
  OS='linux-x86-64'
fi

cd target/
if ! ls sonarqube-*/bin/$OS/sonar.sh &> /dev/null; then
  unzip sonarqube-*.zip
fi
cd sonarqube-*
bin/$OS/sonar.sh restart
sleep 3
tail -n1000 -f logs/sonar.log
