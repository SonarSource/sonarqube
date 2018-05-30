#!/usr/bin/env bash

set -euo pipefail

DUMP_DIR="/tmp/batch_dumps"
SQ_ROOT_URL="http://localhost:9000"

cd $DUMP_DIR
for file in *.zip; do
  base=${file%.zip}
  url=$(cat ${base}.txt)
  echo "base=$base, url=$url"

  curl -u admin:admin -F report=@$DUMP_DIR/${base}.zip ${SQ_ROOT_URL}${url}
done
