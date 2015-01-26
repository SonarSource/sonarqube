#!/bin/sh

OUTPUT_DIR="src/main/gen-java"

mkdir -p ${OUTPUT_DIR}
protoc --proto_path=src/main/protobuf --java_out=${OUTPUT_DIR} src/main/protobuf/*.proto
