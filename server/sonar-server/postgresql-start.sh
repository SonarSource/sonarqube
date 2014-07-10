#!/bin/sh
export MAVEN_OPTS='-Xmx1512m -XX:MaxPermSize=256m'
mvn org.apache.tomcat.maven:tomcat7-maven-plugin::run -Pstart-dev-server,postgresql $*
