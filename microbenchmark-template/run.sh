#!/bin/sh

# The command-line arguments can be used to list the benchmarks to be executed.
# By default all benchmarks are executed.
# Example: run.sh org.sonar.microbenchmark.SerializationBenchmark

mvn clean install
java -jar target/microbenchmark.jar $*
