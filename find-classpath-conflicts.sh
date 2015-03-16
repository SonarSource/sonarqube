#!/bin/sh

# Search for duplication of classes in classpath
# This check can not be automated in build yet as current
# conflicts must be fixed.

mvn org.basepom.maven:duplicate-finder-maven-plugin:check
