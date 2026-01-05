#!/usr/bin/env bash

set -eo pipefail

# This script regenerates the SQL snapshots for the PureJDBCTests.
# It should be run from the agent/test/jdbc directory.

# Check if ORACLE_URL is set
if [[ -z "${ORACLE_URL:-}" ]]; then
  echo "ERROR: ORACLE_URL environment variable is not set." >&2
  echo "Please set ORACLE_URL to your Oracle database connection string, e.g.:" >&2
  echo "  export ORACLE_URL=\"jdbc:oracle:thin:@localhost:1521\"" >&2
  exit 1
fi

echo "INFO: Running PureJDBCTests to generate new AppMaps..."

# Source helper.bash to get _find_agent_jar function
# Set BATS_TEST_DIR so helper.bash can locate files correctly
export BATS_TEST_DIR="$(pwd)"
source ../helper.bash

find_agent_jar
if [[ -z "$AGENT_JAR" ]]; then
  echo "ERROR: Agent JAR not found by helper.bash. Please ensure the agent is built." >&2
  exit 1
fi

export ORACLE_URL
export AGENT_JAR
# JAVA_HOME is handled by gradlew wrapper

# Run the tests to generate fresh AppMaps
./gradlew -q test --tests 'PureJDBCTests'

echo "INFO: Regenerating raw SQL snapshots..."

SNAPSHOT_DIR="$(pwd)/snapshots"
APPMAP_DIR="$(pwd)/tmp/appmap/junit"

# Clear old snapshots
rm -f "$SNAPSHOT_DIR"/*

# Generate new raw SQL snapshots
for f in "$APPMAP_DIR"/com_example_accessingdatajpa_PureJDBCTests_*.appmap.json; do
  if [ -f "$f" ]; then
    snapshot_name=$(basename "$f" .appmap.json).sql
    jq -r '.events[] | select(.sql_query) | .sql_query.sql' "$f" > "$SNAPSHOT_DIR/$snapshot_name"
  fi
done

echo "INFO: Snapshots regenerated successfully in $SNAPSHOT_DIR"
