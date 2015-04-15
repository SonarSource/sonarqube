#!/bin/bash
###############################
# usage: use -D option to enable remote debugging of the web server on port 5005
###############################

if [[ "$OSTYPE" == "darwin"* ]]; then
  OS='macosx-universal-64'
  SED_DISABLE_BACKUP=" ''"
else
  OS='linux-x86-64'
  SED_DISABLE_BACKUP=""
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

if [ "$1" = "-D" ]; then
  echo "enabling debug in conf/sonar.properties, listening on port 5005"
  sed -i $SED_DISABLE_BACKUP '/javaAdditionalOpts/d' conf/sonar.properties
  echo "" >> conf/sonar.properties
  echo "sonar.web.javaAdditionalOpts=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005" >> conf/sonar.properties
fi

bin/$OS/sonar.sh restart
sleep 1
tail -100f logs/sonar.log
