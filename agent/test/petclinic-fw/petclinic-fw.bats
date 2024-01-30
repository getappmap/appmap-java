#!/usr/bin/env bats

load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'

load '../petclinic-shared/shared-setup.bash'
load '../petclinic-shared/static-resources.bash'
load '../petclinic-shared/message-params.bash'

setup_file() {
  if [[ $JAVA_VERSION != 17.* ]]; then
    skip "needs Java 17"
  fi

  export FIXTURE_DIR=build/fixtures/spring-framework-petclinic
  _shared_setup

  start_petclinic_fw >&3
}

teardown_file() {
  stop_ws
}

setup() {
  rm -rf "${FIXTURE_DIR}/tmp/appmap"
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
  _test_requests_for_nonstatic_resources_are_recorded_by_default
}

@test "request for static resources don't generate recordings" {
  _test_request_for_static_resources_dont_generate_recordings
}

@test "form data is recorded as message parameters" {
  _test_form_data_is_recorded_as_message_parameters
}

@test "the agent doesn't exhaust the InputStream" {
  _test_the_agent_doesnt_exhaust_theInputStream
}