#!/usr/bin/env bats

load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'

setup_file() {
  start_petclinic_fw >&3
}

teardown_file() {
  stop_ws
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

@test "requests are recorded by default" {
  run _curl -sXGET "${WS_URL}/owners/1/pets/1/edit"
  assert_success 
  local dir='build/fixtures/spring-framework-petclinic/tmp/appmap/request_recording'
  
  run bash -o pipefail -c "ls -t ${dir}/*owners_1_pets_1_edit.appmap.json | head -1"
  assert_success

  output="$(<${output})"
  assert_json_eq '.events[] | .http_server_request | .path_info' '/owners/1/pets/1/edit' 
}