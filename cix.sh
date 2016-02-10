#!/bin/bash
#

set -euo pipefail

case "$RUN_ACTIVITY" in

  run-db-unit-tests-*)
    DB_ENGINE=`echo $RUN_ACTIVITY | sed "s/run-db-unit-tests-//g"`

    ./run-db-unit-tests.sh "http://infra.internal.sonarsource.com/jenkins/orch-${DB_ENGINE}.properties"
    ;;

  run-it-released-plugins)
    ./run-integration-tests.sh "http://infra.internal.sonarsource.com/jenkins/orch-h2.properties" "Plugins"
    ;;

  run-perf-tests)
    ./run-perf-tests.sh
    ;;

  *)
    echo "unknown RUN_ACTIVITY = $RUN_ACTIVITY"
    exit 1
    ;;

esac
