#!/bin/sh

export MAVEN_OPTS='-Xmx512m'

echo '-------------------------------------------------'
echo ''
echo ' WARNING - Sonar will be built for FIREFOX ONLY  '
echo ''
echo '-------------------------------------------------'

# it is recommended to use maven 3 for faster builds
mvn clean install -Dtest=false -DfailIfNoTests=false -Pdev
