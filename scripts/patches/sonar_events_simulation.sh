#!/usr/bin/env bash
set -euo pipefail
SQ_HOME=$1

PLUGIN_DIR="private/plugins/sonar-events-simulation-plugin/build/libs"
PLUGIN_JAR=$(ls ${PLUGIN_DIR}/sonar-events-simulation-plugin-*.jar 2>/dev/null | head -1)

if [ -z "$PLUGIN_JAR" ]; then
  echo "sonar-events-simulation-plugin JAR not found. Build it first:"
  echo "  ./gradlew :private:plugins:sonar-events-simulation-plugin:jar"
  exit 1
fi

echo "Installing sonar-events-simulation-plugin into $SQ_HOME/extensions/plugins/"
mkdir -p "$SQ_HOME/extensions/plugins"
cp "$PLUGIN_JAR" "$SQ_HOME/extensions/plugins/"
echo "Done: $(basename $PLUGIN_JAR)"
