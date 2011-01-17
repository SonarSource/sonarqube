#!/bin/sh

export MAVEN_OPTS='-Xmx512m'

# it is recommended to use maven 3 for faster builds
mvn clean install -Dtest=false -DfailIfNoTests=false -Ddev