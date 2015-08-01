#!/bin/bash

# Compiles all the Protocol Buffers files (*.proto) to Java source code.
# Local installation of protobuf compiler is NOT needed.

# Available versions listed at http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.github.os72%22%20AND%20a%3A%22protoc-jar%22
PROTOBUF_VERSION="2.6.1.4"

mvn org.apache.maven.plugins:maven-dependency-plugin::copy -Dartifact=com.github.os72:protoc-jar:$PROTOBUF_VERSION -DoutputDirectory=target

# Usage: compile_protobuf <module> <type: main or test>
function compile_protobuf {
  INPUT="$1/src/$2/protobuf"
  OUTPUT="$1/src/$2/gen-java"

  if [ -d $INPUT ]
  then
    echo "Compiling [$INPUT] to [$OUTPUT]..."
    rm -rf $OUTPUT
    mkdir -p $OUTPUT
    java -jar target/protoc-jar-$PROTOBUF_VERSION.jar --proto_path=$INPUT --java_out=$OUTPUT $INPUT/*.proto
  fi
}

compile_protobuf "sonar-batch-protocol" "main"
compile_protobuf "sonar-core" "test"
compile_protobuf "sonar-db" "main"
compile_protobuf "sonar-ws" "main"



