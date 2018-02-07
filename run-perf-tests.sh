#!/bin/bash
set -euo pipefail

echo 'Run performance tests'
cd tests
mvn verify -B -e -V -Dcategory=ServerPerformance $*
