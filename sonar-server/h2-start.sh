#!/bin/sh
export MAVEN_OPTS='-Xmx768m -XX:MaxPermSize=128m'
mvn clean org.apache.tomcat.maven:tomcat7-maven-plugin::run -Pstart-dev-server,h2 $*
