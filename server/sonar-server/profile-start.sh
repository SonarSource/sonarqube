#!/bin/sh
export MAVEN_OPTS='-Xmx512m -XX:MaxPermSize=160m -agentpath:/Applications/jprofiler8/bin/macos/libjprofilerti.jnilib=port=8849 '
mvn org.apache.tomcat.maven:tomcat7-maven-plugin::run -Pstart-dev-server,h2 $*
