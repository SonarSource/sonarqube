#!/bin/sh
mvn org.codehaus.mojo:license-maven-plugin:aggregate-add-third-party -pl sonar-application,sonar-maven-plugin -am
