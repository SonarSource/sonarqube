#!/bin/sh

export MAVEN_OPTS='-Xmx256m'

echo '-------------------------------------------------'
echo ''
echo ' WARNINGS' 
echo '' 
echo ' Unit tests are NOT executed.'
echo ''
echo '-------------------------------------------------'

mvn clean install -Dtest=false -DfailIfNoTests=false $*
