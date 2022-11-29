#!/usr/bin/env bats

load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'

setup_file() {
  cd test/jdbc
  ./gradlew -is clean
}

@test "it works" {
  ./gradlew -is test 

  output="$(<tmp/appmap/junit/com_example_accessingdatajpa_CustomerRepositoryTests_testFindFromBogusTable.appmap.json)"
  assert_json_eq '.events | length' 4
  assert_json_eq '.events[2].exceptions | length' 1
  assert_json_eq '.events[2].exceptions[0].class' org.h2.jdbc.JdbcSQLSyntaxErrorException
}

