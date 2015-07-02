#!/bin/bash

##############################
#
# Generates batch reports dumps of a specific project into a given dump repository
# from the Git history of that project.
#
# This script runs the analysis of the project in the current directory of the N
# last commits of the current project. N can be configured.
#
# Batch report dumps can be processed with the replay_batch.sh script
#
#
##############################


set -euo pipefail

function clean() {
  rm -Rf $BUILD_DIR
}

function createPrivateClone() {
  BRANCH=$(git symbolic-ref -q HEAD)
  BRANCH=${BRANCH##refs/heads/}
  BRANCH=${BRANCH:-HEAD}
  LOCATION=$(pwd)
  REPO_DIR=$(git rev-parse --show-toplevel)
  REPO_NAME=${REPO_DIR##*/}
  RELATIVE_PATH=${LOCATION#$REPO_DIR}

  # create temp directory and register trap to clean it on exit
  BUILD_DIR=$(mktemp -d -t ${REPO_NAME}.XXXXXXXX 2>/dev/null)
  trap "clean" INT TERM EXIT

  # Private build
  cd $REPO_DIR
  git clone --single-branch -slb "${BRANCH}" . ${BUILD_DIR}
  cd ${BUILD_DIR}${RELATIVE_PATH}
}

DUMP_DIR="/tmp/batch_dumps"
HISTORY_LENGTH=30
SONAR_HOST="http://localhost:9000"
SONAR_USER="admin"
SONAR_PASSWORD="admin"
SONAR_JDBC_URL="jdbc:postgresql://localhost:5432/sonar"
SONAR_JDBC_USERNAME="sonar"
SONAR_JDBC_PASSWORD="sonar"

createPrivateClone

# retrieve ${HISTORY_LENGTH} commits for the current directory
git log -${HISTORY_LENGTH} --pretty=%h -- . | tac > /tmp/sha1s.txt

git co -b working_branch 
while read sha1; do
  echo $sha1
  git reset --hard $sha1
  date=`git show -s --format=%ci | sed 's/ /T/' | sed 's/ //'`

  echo ""
  echo "======================== analyzing at $date ($sha1) ======================================="
  $M2_HOME/bin/mvn sonar:sonar -Dsonar.host.url=$SONAR_HOST -Dsonar.login=$SONAR_USER -Dsonar.password=$SONAR_PASSWORD -Dsonar.analysis.mode=analysis -Dsonar.issuesReport.html.enable= -Dsonar.issuesReport.console.enable= -Dsonar.jdbc.url=$SONAR_JDBC_URL -Dsonar.jdbc.username=$SONAR_JDBC_USERNAME -Dsonar.jdbc.password=$SONAR_JDBC_PASSWORD -Dsonar.batch.dumpReportDir=$DUMP_DIR -Dsonar.projectDate=$date
  
done < /tmp/sha1s.txt

