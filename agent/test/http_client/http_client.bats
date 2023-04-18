#!/usr/bin/env bats

load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'

setup_file() {
  start_petclinic
}

teardown_file() {
  stop_petclinic
}

@test "requests are recorded" {
  pushd test/http_client

  run ./gradlew -q run ${DEBUG} --args "${WS_URL}"

  assert_json_eq '.events[1].http_client_request.request_method' "GET"
  assert_json_eq '.events[1].http_client_request.url' "${WS_URL}/vets.html"

  assert_json_eq '.events[2].http_client_response.status' "200"
}

