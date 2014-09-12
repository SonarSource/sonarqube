#!/bin/sh
export MAVEN_OPTS='-Xmx256m'
mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent install -B -e -V -Pcoverage-per-test -Dmaven.test.failure.ignore=true
