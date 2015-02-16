#!/bin/sh

# Parallel executions of maven modules and tests.
# Half of CPU core are used in to keep other half for OS and other programs.
# As long as web JS tests are not stable, it's recommended to execute: quick-build.sh -DskipWebTests=true
mvn clean install -e -B -T0.5C -DforkCount=0.5C $*
