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
    CATEGORY_GROUP=$(sed "s/run-db-integration-tests-//g" <<< $RUN_ACTIVITY | cut -d \- -f 2)

    if [[ "$GITHUB_BRANCH" == "PULLREQUEST-"* ]] && [[ "$DB_ENGINE" != "postgresql93" ]]; then
      # execute PR QA only on postgres
      exit 0
    else
      mvn clean package -B -e -V -f tests/plugins/pom.xml

      case "$CATEGORY_GROUP" in
        Category1)
          CATEGORY="Category1|authorization|measure|qualityGate|source"
          ;;

        Category2)
          CATEGORY="issue|test|qualityModel"
          ;;

        Category3)
          CATEGORY="Category3|component|project"
          ;;

        Category4)
          CATEGORY="Category4|duplication"
          ;;

        Category5)
          CATEGORY="Category5"
          ;;

        Category6)
          CATEGORY="Category6|organization"
          ;;

        *)
          echo "unknown CATEGORY_GROUP: $CATEGORY_GROUP"
          exit 1
          ;;
      esac

      mvn verify \
          -f tests/pom.xml \
          -Dcategory="$CATEGORY" \
          -Dorchestrator.configUrl="http://infra.internal.sonarsource.com/jenkins/orch-$DB_ENGINE.properties" \
          -Pwith-db-drivers \
          -B -e -V
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
    if [[ "$GITHUB_BRANCH" == "PULLREQUEST-"* ]] && [[ "$DB_ENGINE" != "postgresql93" ]]; then
     exit 0
    else    
      ./run-upgrade-tests.sh "http://infra.internal.sonarsource.com/jenkins/orch-${DB_ENGINE}.properties"
    fi
    ;;

  *)
    echo "unknown RUN_ACTIVITY = $RUN_ACTIVITY"
    exit 1
    ;;

esac
