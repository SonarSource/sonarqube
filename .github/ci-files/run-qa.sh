#!/bin/bash

# Convert first argument to lowercase (e.g., Postgres -> postgres)
ORCHESTRATOR_CONFIG=${1,,}

BUILD_TASK=":private:it-core:testClasses"
TEST_TASK=":private:it-core:integrationTest"

if [ "${QA_CATEGORY}" == "EE1" ] || [ "${QA_CATEGORY}" == "EE2" ]; then
  BUILD_TASK=":private:it-enterprise:it-tests:testClasses"
  TEST_TASK=":private:it-enterprise:it-tests:integrationTest"

  if [ "${QA_CATEGORY}" == "EE1" ]; then
    BUILD_TASK="${BUILD_TASK} :private:core-extension-scim:bbtClasses"
    TEST_TASK="${TEST_TASK} :private:core-extension-scim:blackBoxTest"
  fi
fi
if [ "${QA_CATEGORY}" == "DE1" ] || [ "${QA_CATEGORY}" == "DE2" ]; then
  BUILD_TASK=":private:core-extension-developer-server:bbtClasses"
  TEST_TASK=":private:core-extension-developer-server:blackBoxTest"

  if [ "${QA_CATEGORY}" == "DE1" ]; then
    BUILD_TASK="${BUILD_TASK} :private:it-developer:it-tests:testClasses"
    TEST_TASK="${TEST_TASK} :private:it-developer:it-tests:integrationTest"
  fi
fi

if [ "${QA_CATEGORY}" == "Architecture" ]; then
  BUILD_TASK=":private:core-extension-architecture:bbtClasses"
  TEST_TASK=":private:core-extension-architecture:blackBoxTest"
fi

if [ "${QA_CATEGORY}" == "SQS_common" ]; then
  BUILD_TASK=":private:core-extension-common:bbtClasses"
  TEST_TASK=":private:core-extension-common:blackBoxTest"
fi

if [ "${QA_CATEGORY}" == "SCA" ]; then
  BUILD_TASK=":private:core-extension-sca:bbtClasses"
  TEST_TASK=":private:core-extension-sca:blackBoxTest"
fi

if [ "${QA_CATEGORY}" == "FixSuggestions" ]; then
  BUILD_TASK=":private:core-extension-fix-suggestions:bbtClasses"
  TEST_TASK=":private:core-extension-fix-suggestions:blackBoxTest"
fi

if [ "${QA_CATEGORY}" == "Branch1" ] || [ "${QA_CATEGORY}" == "Branch2" ] || [ "${QA_CATEGORY}" == "BITBUCKET" ] || [ "${QA_CATEGORY}" == "BITBUCKET_CLOUD" ] ||
  [ "${QA_CATEGORY}" == "GITLAB" ] || [ "${QA_CATEGORY}" == "GITLAB_CLOUD" ] || [ "${QA_CATEGORY}" == "AZURE" ] ||
  [ "${QA_CATEGORY}" == "GITHUB" ] || [ "${QA_CATEGORY}" == "GITHUB_PROVISIONING" ] || [ "${QA_CATEGORY}" == "GITHUB_SLOW_TESTS" ]; then
  BUILD_TASK=":private:it-branch:it-tests:testClasses"
  TEST_TASK=":private:it-branch:it-tests:integrationTest"
fi
if [ "${QA_CATEGORY}" == "License1" ] || [ "${QA_CATEGORY}" == "License2" ]; then
  BUILD_TASK=":private:it-license:it-tests:testClasses"
  TEST_TASK=":private:it-license:it-tests:integrationTest"
fi
if [ "${QA_CATEGORY}" == "HA_CLUSTER" ] || [ "${QA_CATEGORY}" == "HA_ELASTICSEARCH" ]; then
  BUILD_TASK=":private:it-ha:it-tests:testClasses"
  TEST_TASK=":private:it-ha:it-tests:integrationTest"
fi
if [ "${QA_CATEGORY}" == "AnalysisPerformance" ]; then
  BUILD_TASK=":private:it-performance:it-tests:testClasses"
  TEST_TASK=":private:it-performance:it-tests:integrationTest"
fi
if [ "${QA_CATEGORY}" == "MIGRATION" ]; then
  BUILD_TASK=":server:sonar-db-migration:testClasses"
  TEST_TASK=":server:sonar-db-migration:test"
fi

if [ "${QA_CATEGORY}" == "GITLAB_PROVISIONING" ]; then
  BUILD_TASK=":private:core-extension-gitlab-provisioning:bbtClasses"
  TEST_TASK=":private:core-extension-gitlab-provisioning:blackBoxTest"
fi
if [ "${QA_CATEGORY}" == "OPEN_API" ]; then
  BUILD_TASK=":private:api-hub:testClasses"
  TEST_TASK=":private:api-hub:integrationTest"
fi

set +e

./gradlew $BUILD_TASK \
  -Dazure.pat="${AZURE_PAT}" \
  -Dgitlab.pat="${GITLAB_PAT}" \
  -Pqa \
  --console plain --build-cache --parallel --profile -x jar

# buildNumber is required for orchestrator to fetch the proper version of SonarQube
./gradlew $TEST_TASK \
  -Dwebdriver.chrome.driver=/usr/bin/chromedriver \
  -DbuildNumber=$BUILD_NUMBER \
  -Dcategory=$QA_CATEGORY \
  -Dorchestrator.configUrl=file:///$GITHUB_WORKSPACE/.github/ci-files/config/orchestrator-$ORCHESTRATOR_CONFIG.properties \
  -Dazure.pat="${AZURE_PAT}" \
  -Dgitlab.pat="${GITLAB_PAT}" \
  -Pqa \
  --console plain --build-cache --profile -x jar

gradle_return_code=$?
set -e

exit $gradle_return_code
