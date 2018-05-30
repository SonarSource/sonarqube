#!/usr/bin/env bash

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

clean() {
  rm -Rf $BUILD_DIR
}

createPrivateClone() {
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

showHelp() {
  echo "Usage: $0 -d DIR_PATH [ -l integer ] [ -h URL ] [ -u string ] [ -p string ]
 -d : path to directory where batch report bumpds will be created
 -l : number of commit in the past (optional: default is $HISTORY_LENGTH)
 -h : URL of the SQ instance (optional: default is $SONAR_HOST)
 -u : username to authentication on the SQ instance (optional: default is $SONAR_USER)
 -p : password to authentication on the SQ instance (optional: default is $SONAR_USER)"
}

checkOptions() {
  if [[ -z "$DUMP_DIR" ]]; then
    >&2 echo "-d option is mandatory"
    showHelp
    exit 1
  fi
}

DUMP_DIR=""
HISTORY_LENGTH=30
SONAR_HOST="http://localhost:9000"
SONAR_USER="admin"
SONAR_PASSWORD="admin"
SONAR_JDBC_URL="jdbc:postgresql://localhost:5432/sonar"
SONAR_JDBC_USERNAME="sonar"
SONAR_JDBC_PASSWORD="sonar"

while getopts ":d:l:h:u:p:" opt; do
  case "$opt" in
    d) DUMP_DIR=$OPTARG
      ;;
    l) HISTORY_LENGTH=$OPTARG
      ;;
    h) SONAR_HOST=$OPTARG
      ;;
    u) SONAR_USER=$OPTARG
      ;;
    p) SONAR_PASSWORD=$OPTARG
      ;;
    :)
      >&2 echo "option $OPTARG requires an argument"
      showHelp
      exit 1
      ;;
    \?)
      >&2 echo "Unsupported option $OPTARG"
      showHelp
      exit 1 
      ;;
  esac
done
checkOptions

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

