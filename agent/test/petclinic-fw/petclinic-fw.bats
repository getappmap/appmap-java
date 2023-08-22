#!/usr/bin/env bats

load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'

load '../petclinic-shared/static-resources.bash'

setup_file() {
  start_petclinic_fw >&3
  export FIXTURE_DIR=build/fixtures/spring-framework-petclinic
}

teardown_file() {
  stop_ws
}

setup() {
  rm -rf "${FIXTURE_DIR}/target/tmp/appmap"
}

@test "remote recording works" { 
  run _curl -sXGET "${WS_URL}/_appmap/record"
  assert_success
  assert_json_eq '.enabled' 'false'

  run _curl -sXPOST "${WS_URL}/_appmap/record"
  assert_success

  _curl -sXGET "${WS_URL}"

  run _curl -sXDELETE "${WS_URL}/_appmap/record"
  assert_success

  assert_json '.events'
  assert_json '.metadata'
  assert_json '.classMap'
}

@test "requests for non-static resources are recorded by default" {
  test_requests_for_nonstatic_resources_are_recorded_by_default
}

@test "request for static resources don't generate recordings" {
  test_request_for_static_resources_dont_generate_recordings
}