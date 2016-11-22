#!/bin/sh
mvn org.codehaus.mojo:license-maven-plugin:aggregate-add-third-party -Dlicense.includedScopes=compile -pl sonar-application -am

cat target/generated-sources/license/THIRD-PARTY.txt
