#!/usr/bin/env bats

load '../helper'
load 'helper'

setup_file() {
  cd "$BATS_TEST_DIRNAME" || exit 1
  _configure_logging

  gradlew -q clean
}

setup() {
  rm -rf tmp/appmap
}

@test "h2 successful test" {
  run gradlew -q test --tests 'CustomerRepositoryTests.testFindFromBogusTable' --rerun-tasks
  assert_success

  output="$(<./tmp/appmap/junit/com_example_accessingdatajpa_CustomerRepositoryTests_testFindFromBogusTable.appmap.json)"
  assert_json_eq '.metadata.test_status' succeeded
  assert_json_eq '.events | length' 4
  assert_json_eq '.events[2].exceptions | length' 3
  assert_json_eq '.events[2].exceptions[2].class' org.h2.jdbc.JdbcSQLSyntaxErrorException
}

@test "h2 failing test" {
  run gradlew -q test --tests 'CustomerRepositoryTests.testFails' --rerun-tasks
  assert_failure

  output="$(<./tmp/appmap/junit/com_example_accessingdatajpa_CustomerRepositoryTests_testFails.appmap.json)"
  assert_json_eq '.metadata.test_status' failed
  assert_json_eq '.metadata.test_failure.message' 'expected: <true> but was: <false>'
}

# Requires a running Oracle instance.
# Locally: docker-compose up -d (in agent/test/jdbc)
# CI: Service is configured in .github/workflows/build-and-test.yml
@test "oracle jpa test" {
  requires_oracle
  run gradlew -q test --tests 'OracleRepositoryTests' --rerun-tasks
  assert_success

  map_file="tmp/appmap/junit/com_example_accessingdatajpa_OracleRepositoryTests_testFindByLastName.appmap.json"
  [ -f "$map_file" ]
  output="$(<"$map_file")"
  assert_json_eq '.metadata.test_status' succeeded
  event_count=$(echo "$output" | jq '.events | length')
  if [ "$event_count" -le 0 ]; then
    echo "Expected event count to be greater than 0, but it was $event_count"
    return 1
  fi
}

# To regenerate the SQL snapshots, run ./regenerate_jdbc_snapshots.sh from this directory.

@test "h2 pure jdbc test suite (snapshot)" {
  export -n ORACLE_URL
  run gradlew -q test --tests 'PureJDBCTests' --rerun-tasks
  assert_success

  local appmap_dir="tmp/appmap/junit"
  local snapshot_dir="snapshots/h2"
  local test_output_dir
  test_output_dir="$(mktemp -d)"

  generate_sql_snapshots "$appmap_dir" "$test_output_dir" "com_example_accessingdatajpa_PureJDBCTests_*.appmap.json"

  run assert_all_calls_returned "$appmap_dir"/*.appmap.json
  assert_success
  run diff -u <(cd "$snapshot_dir" && grep -ri . | sort -s -t: -k1,1) <(cd "$test_output_dir" && grep -ri . | sort -s -t: -k1,1)
  assert_success "Snapshot mismatch"

  rm -rf "$test_output_dir"
}

@test "oracle pure jdbc test suite (snapshot)" {
  requires_oracle
  export ORACLE_URL
  run gradlew -q test --tests 'PureJDBCTests' --rerun-tasks
  assert_success

  local appmap_dir="tmp/appmap/junit"
  local snapshot_dir="snapshots/oracle"
  local test_output_dir
  test_output_dir="$(mktemp -d)"

  generate_sql_snapshots "$appmap_dir" "$test_output_dir" "com_example_accessingdatajpa_PureJDBCTests_*.appmap.json"

  run assert_all_calls_returned "$appmap_dir"/*.appmap.json
  assert_success
  run diff -u <(cd "$snapshot_dir" && grep -ri . | sort -s -t: -k1,1) <(cd "$test_output_dir" && grep -ri . | sort -s -t: -k1,1)
  assert_success "Snapshot mismatch"

  rm -rf "$test_output_dir"
}
