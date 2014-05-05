#!/bin/sh

if ! ls sonar-application/target/sonarqube-*.zip &> /dev/null; then
  echo 'Sources are not built'
  ./quick-build.sh
fi

cd sonar-application/target/
if ! ls sonarqube-*/bin/macosx-universal-64/sonar.sh &> /dev/null; then
  unzip sonarqube-*.zip
fi
cd sonarqube-*
bin/macosx-universal-64/sonar.sh restart
sleep 1
tail -100f logs/sonar.log
