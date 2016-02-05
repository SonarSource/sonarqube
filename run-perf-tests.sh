#!/bin/bash
set -euo pipefail

echo 'Run performance tests'
cd it/perf-tests
mvn verify -B -e -V $*
