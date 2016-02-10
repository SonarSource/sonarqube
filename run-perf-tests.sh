#!/bin/bash
set -euo pipefail

echo 'Run performance tests'
cd tests/perf
mvn verify -B -e -V $*
