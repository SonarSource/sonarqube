#!/bin/sh


cd sonar-application/target/
if ! ls sonarqube-*/bin/sonar-application*.jar &> /dev/null; then
  unzip sonarqube-*.zip
fi

cd sonarqube-*
java -jar ./lib/sonar-application*.jar -Dsonar.web.javaAdditionalOpts=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
