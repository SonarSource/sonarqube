#!/bin/sh

export MAVEN_OPTS='-Xmx256m'

echo '-------------------------------------------------'
echo ''
echo ' WARNINGS' 
echo '' 
echo ' Unit tests are NOT executed.'
echo ' Build for FIREFOX ONLY.'
echo ''
echo '-------------------------------------------------'

# it is recommended to use maven 3 for faster builds
mvn clean install -Dtest=false -DfailIfNoTests=false $*
