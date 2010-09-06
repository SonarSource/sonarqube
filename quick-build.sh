#!/bin/sh

# it is recommended to use maven 3.0-beta-1 (or more) for faster builds
mvn clean install -Dtest=false -DfailIfNoTests=false