#!/bin/bash
# QA pipeline

set -euo pipefail

case "$RUN_ACTIVITY" in

  run-db-unit-tests-*)
    DB_ENGINE=$(sed "s/run-db-unit-tests-//g" <<< $RUN_ACTIVITY)

    ./run-db-unit-tests.sh "http://infra.internal.sonarsource.com/jenkins/orch-${DB_ENGINE}.properties"
    ;;

  run-db-integration-tests-*)
    DB_ENGINE=$(sed "s/run-db-integration-tests-//g" <<< $RUN_ACTIVITY | cut -d \- -f 1)
    CATEGORY=$(sed "s/run-db-integration-tests-//g" <<< $RUN_ACTIVITY | cut -d \- -f 2)

    ./run-integration-tests.sh "${CATEGORY}" "http://infra.internal.sonarsource.com/jenkins/orch-${DB_ENGINE}.properties"
    ;;

  run-it-released-plugins)
    ./run-integration-tests.sh "Plugins" "http://infra.internal.sonarsource.com/jenkins/orch-h2.properties"
    ;;

  run-perf-tests)
    ./run-perf-tests.sh
    ;;

  run-upgrade-tests-*)
    DB_ENGINE=$(sed "s/run-upgrade-tests-//g" <<< $RUN_ACTIVITY)
    ./run-upgrade-tests.sh "http://infra.internal.sonarsource.com/jenkins/orch-${DB_ENGINE}.properties"
    ;;

  *)
    echo "unknown RUN_ACTIVITY = $RUN_ACTIVITY"
    exit 1
    ;;

esac
