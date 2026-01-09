#!/usr/bin/env bats

load '../helper'

setup_file() {
  cd test/jdbc
  _configure_logging

  gradlew -q clean
}

setup() {
  rm -rf tmp/appmap
}

@test "successful test" {
  run gradlew -q test --tests 'CustomerRepositoryTests.testFindFromBogusTable'
  assert_success

  output="$(<./tmp/appmap/junit/com_example_accessingdatajpa_CustomerRepositoryTests_testFindFromBogusTable.appmap.json)"
  assert_json_eq '.metadata.test_status' succeeded
  assert_json_eq '.events | length' 6
  assert_json_eq '.events[3].exceptions | length' 1
  assert_json_eq '.events[3].exceptions[0].class' org.h2.jdbc.JdbcSQLSyntaxErrorException
}

@test "failing test" {
  run gradlew -q test --tests 'CustomerRepositoryTests.testFails'
  assert_failure

  output="$(<./tmp/appmap/junit/com_example_accessingdatajpa_CustomerRepositoryTests_testFails.appmap.json)"
  assert_json_eq '.metadata.test_status' failed
  assert_json_eq '.metadata.test_failure.message' 'expected: <true> but was: <false>'
}



