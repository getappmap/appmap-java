#!/usr/bin/env bats

# To regenerate the SQL snapshots, run ./regenerate_jdbc_snapshots.sh from this directory.

load '../helper'

setup_file() {
  cd "$BATS_TEST_DIR" || true
  _configure_logging

  ./gradlew -q clean
}

setup() {
  rm -rf tmp/appmap
}

@test "h2 successful test" {
  run ./gradlew -q test --tests 'CustomerRepositoryTests.testFindFromBogusTable'
  assert_success

  output="$(<./tmp/appmap/junit/com_example_accessingdatajpa_CustomerRepositoryTests_testFindFromBogusTable.appmap.json)"
  assert_json_eq '.metadata.test_status' succeeded
  assert_json_eq '.events | length' 4
  assert_json_eq '.events[2].exceptions | length' 3
  assert_json_eq '.events[2].exceptions[2].class' org.h2.jdbc.JdbcSQLSyntaxErrorException
}

@test "h2 failing test" {
  run ./gradlew -q test --tests 'CustomerRepositoryTests.testFails'
  assert_failure

  output="$(<./tmp/appmap/junit/com_example_accessingdatajpa_CustomerRepositoryTests_testFails.appmap.json)"
  assert_json_eq '.metadata.test_status' failed
  assert_json_eq '.metadata.test_failure.message' 'expected: <true> but was: <false>'
}

# Requires a running Oracle instance.
# Locally: docker-compose up -d (in agent/test/jdbc)
# CI: Service is configured in .github/workflows/build-and-test.yml
@test "oracle jpa test" {
  run ./gradlew -q test --tests 'OracleRepositoryTests'
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

@test "oracle pure jdbc test suite (snapshot)" {
  run ./gradlew -q test --tests 'PureJDBCTests'
  assert_success

  # Verify that the list of generated appmaps corresponds to the list of snapshots.
  appmap_list=$(ls tmp/appmap/junit/com_example_accessingdatajpa_PureJDBCTests_*.appmap.json | xargs -n 1 basename | sed 's/\.appmap\.json$//' | sort)
  snapshot_list=$(ls snapshots/*.sql | xargs -n 1 basename | sed 's/\.sql$//' | sort)

  run diff -u <(echo "$appmap_list") <(echo "$snapshot_list")
  assert_success "Mismatch between generated AppMaps and snapshots"

  for f in tmp/appmap/junit/com_example_accessingdatajpa_PureJDBCTests_*.appmap.json; do
    snapshot_file="snapshots/$(basename "$f" .appmap.json).sql"
    [ -f "$snapshot_file" ] || { echo "Snapshot file not found: $snapshot_file"; return 1; }

    new_output_file=$(mktemp)
    jq -r '.events[] | select(.sql_query) | .sql_query.sql' "$f" > "$new_output_file"

    run diff -u "$snapshot_file" "$new_output_file"
    assert_success "Snapshot mismatch for $(basename "$f")"

    rm "$new_output_file"
  done
}





