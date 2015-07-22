#!/bin/bash

# Usage: compile_protobuf <inputDir> <outputDir>
function compile_protobuf {
  echo "Compiling [$1] to [$2]..."
  mkdir -p $2
  protoc --proto_path=$1 --java_out=$2 $1/*.proto
}

compile_protobuf "sonar-core/src/test/protobuf" "sonar-core/src/test/gen-java"
compile_protobuf "sonar-batch-protocol/src/main/protobuf" "sonar-batch-protocol/src/main/gen-java"



