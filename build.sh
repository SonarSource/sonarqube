#!/bin/sh

./stop.sh

# Parallel executions of maven modules and tests.
# Half of CPU core are used in to keep other half for OS and other programs.
mvn clean install -e -B -T0.5C -DforkCount=0.5C $*
