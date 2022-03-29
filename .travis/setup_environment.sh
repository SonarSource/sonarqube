#!/bin/bash
# Sets up the environment to be able to send notifications to burgr
# use generic environments to remove coupling with Travis ; see setup_promote_environment

export GITHUB_REPO=${TRAVIS_REPO_SLUG}
export BUILD_NUMBER=$TRAVIS_BUILD_NUMBER
export PIPELINE_ID=${BUILD_NUMBER}
if [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
  export GIT_SHA1=${TRAVIS_COMMIT} # $CIRRUS_CHANGE_IN_REPO
  export GIT_BRANCH=$TRAVIS_BRANCH
  export STAGE_TYPE="branch"
  export STAGE_ID=${GIT_BRANCH}
else
  export GIT_SHA1=${TRAVIS_PULL_REQUEST_SHA}
  export GIT_BRANCH=$TRAVIS_PULL_REQUEST_BRANCH
  export PULL_REQUEST_BASE_BRANCH=$TRAVIS_BRANCH
  export PULL_REQUEST_NUMBER=$TRAVIS_PULL_REQUEST
  export STAGE_TYPE="pr_number"
  export STAGE_ID=${PULL_REQUEST_NUMBER}
fi
echo "======= SHA1 is ${GIT_SHA1} on branch '${GIT_BRANCH}'. Burgr stage '${STAGE_TYPE} with stage ID '${STAGE_ID} ======="
