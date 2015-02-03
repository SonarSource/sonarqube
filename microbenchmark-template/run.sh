#!/bin/sh

mvn clean install
java -jar target/microbenchmark.jar -i 5 -wi 5 -f 5
