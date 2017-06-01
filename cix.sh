#!/bin/bash
# QA pipeline

set -euo pipefail

case "$RUN_ACTIVITY" in

  run-db-unit-tests-*)
    DB_ENGINE=$(sed "s/run-db-unit-tests-//g" <<< $RUN_ACTIVITY)
    if [ "${GITHUB_BRANCH}" == "PULLREQUEST-2104" ] ||  [ "${DB_ENGINE}" == "postgresql93" ]
    then
      ./run-db-unit-tests.sh "http://infra.internal.sonarsource.com/jenkins/orch-${DB_ENGINE}.properties"
    fi
    ;;

  run-db-integration-tests-*)
    DB_ENGINE=$(sed "s/run-db-integration-tests-//g" <<< $RUN_ACTIVITY | cut -d \- -f 1)
    CATEGORY=$(sed "s/run-db-integration-tests-//g" <<< $RUN_ACTIVITY | cut -d \- -f 2)

    if [ "${GITHUB_BRANCH}" == "PULLREQUEST-2104" ] ||  [ "${DB_ENGINE}" == "postgresql93" ]
    then
      ./run-integration-tests.sh "${CATEGORY}" "http://infra.internal.sonarsource.com/jenkins/orch-${DB_ENGINE}.properties"
    fi
    ;;

  run-integration-tests-*)
    CATEGORY=$(sed "s/run-integration-tests-//g" <<< $RUN_ACTIVITY)
    if [ "${GITHUB_BRANCH}" == "PULLREQUEST-2104" ]
    then
      ./run-integration-tests.sh "${CATEGORY}" "http://infra.internal.sonarsource.com/jenkins/orch-embedded.properties"
    fi
    ;;

  run-it-released-plugins)
    ./run-integration-tests.sh "Plugins" "http://infra.internal.sonarsource.com/jenkins/orch-h2.properties"
    ;;

  run-perf-tests)
    ./run-perf-tests.sh
    ;;

  run-upgrade-tests-*)
    DB_ENGINE=$(sed "s/run-upgrade-tests-//g" <<< $RUN_ACTIVITY)
    if [ "${GITHUB_BRANCH}" == "PULLREQUEST-2104" ] ||  [ "${DB_ENGINE}" == "postgresql93" ]
    then
      ./run-upgrade-tests.sh "http://infra.internal.sonarsource.com/jenkins/orch-${DB_ENGINE}.properties"
    fi
    ;;

  *)
    echo "unknown RUN_ACTIVITY = $RUN_ACTIVITY"
    exit 1
    ;;

esac
