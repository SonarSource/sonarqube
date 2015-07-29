#!/bin/bash

# Compiles all the Protocol Buffers files (*.proto) to Java source code.
# IMPORTANT - protobuf 2.6.1 must be installed. Other versions are not supported.

# Usage: compile_protobuf <module> <type: main or test>
function compile_protobuf {
  INPUT="$1/src/$2/protobuf"
  OUTPUT="$1/src/$2/gen-java"

  if [ -d $INPUT ]
  then
    echo "Compiling [$INPUT] to [$OUTPUT]..."
    rm -rf $OUTPUT
    mkdir -p $OUTPUT
    protoc --proto_path=$INPUT --java_out=$OUTPUT $INPUT/*.proto
  fi
}

compile_protobuf "sonar-batch-protocol" "main"
compile_protobuf "sonar-core" "test"
compile_protobuf "sonar-db" "main"
compile_protobuf "sonar-ws" "main"



