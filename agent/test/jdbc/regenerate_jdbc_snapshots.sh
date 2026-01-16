#!/usr/bin/env bash

set -eo pipefail

# This script regenerates the SQL snapshots for the PureJDBCTests.
# It should be run from the agent/test/jdbc directory.
#
# Usage:
#   ./regenerate_jdbc_snapshots.sh           # Regenerate H2 snapshots
#   ORACLE_URL=... ./regenerate_jdbc_snapshots.sh  # Regenerate Oracle snapshots

# Source helper.bash to get _find_agent_jar function
# Set BATS_TEST_DIR so helper.bash can locate files correctly
export BATS_TEST_DIR="$(pwd)"
source ../helper.bash
source ./helper.bash

find_agent_jar
if [[ -z "$AGENT_JAR" ]]; then
  echo "ERROR: Agent JAR not found by helper.bash. Please ensure the agent is built." >&2
  exit 1
fi

export AGENT_JAR

regenerate_snapshots() {
  local db_type="$1"
  local snapshot_dir="$PWD/snapshots/$db_type"
  local appmap_dir="$PWD/tmp/appmap/junit"

  echo "INFO: Regenerating $db_type snapshots..."

  # Clear old snapshots and appmap dirs
  rm -f "$snapshot_dir"/*
  rm -f "$appmap_dir"/com_example_accessingdatajpa_PureJDBCTests_*.appmap.json

  # Run the tests to generate fresh AppMaps
  ../gradlew -q test --tests 'PureJDBCTests' --rerun-tasks

  echo "INFO: Generating raw SQL snapshots for $db_type..."

  # Generate new raw SQL snapshots
  generate_sql_snapshots "$appmap_dir" "$snapshot_dir" "com_example_accessingdatajpa_PureJDBCTests_*.appmap.json"

  echo "INFO: $db_type snapshots regenerated successfully in $snapshot_dir"
}

if [[ -z "${ORACLE_URL:-}" ]]; then
  echo "WARNING: ORACLE_URL is not set. Skipping Oracle snapshot regeneration." >&2
  echo "To regenerate Oracle snapshots, set ORACLE_URL and run this script again." >&2
else
  export ORACLE_URL
  regenerate_snapshots "oracle"
fi

unset ORACLE_URL
regenerate_snapshots "h2"
