#!/usr/bin/env bats

load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'

setup_file() {
  cp test/http_client/NoContentController.java build/fixtures/spring-petclinic/src/main/java/org/springframework/samples/petclinic/system/.
  start_petclinic >&3

  pushd test/http_client
}

teardown_file() {
  popd

  stop_ws
}

@test "request without query" {
  run ./gradlew -q -PmainClass=http_client.HttpClientTest run ${DEBUG} --args "${WS_URL}/vets"

  assert_json_eq '.events[1].http_client_request.request_method' "GET"
  assert_json_eq '.events[1].http_client_request.url' "${WS_URL}/vets"

  # Neither message nor parameters should set set
  assert_json_eq '.events[1].message' ''
  assert_json_eq '.events[1].parameters' ''
}

@test "request with query" {
  run ./gradlew -q -PmainClass=http_client.HttpClientTest run ${DEBUG} --args "${WS_URL}/owners?lastName=davis"

  assert_json_eq '.events[1].http_client_request.url' "${WS_URL}/owners"
  assert_json_eq '.events[1].message | length' 1
  assert_json_eq '.events[1].message[0] | "\(.name) \(.value)"' "lastName davis"
  
  assert_json_eq '.events[2].http_client_response.status' "200"
}

@test "request without Content-Type" {
  run ./gradlew -q -PmainClass=http_client.HttpClientTest run ${DEBUG} --args "${WS_URL}/no-content"

  assert_json_eq '.events[1].http_client_request.url' "${WS_URL}/no-content"
  assert_json_eq '.events[2].http_client_response.status' "200"
}

@test "request with HttpHost" {
  run ./gradlew -q -PmainClass=http_client.HttpHostTest run ${DEBUG} --args "${WS_HOST} ${WS_PORT} /owners?lastName=davis"
  
  assert_json_eq '.events[1].http_client_request.url' "${WS_URL}/owners"
  assert_json_eq '.events[1].message | length' 1
  assert_json_eq '.events[1].message[0] | "\(.name) \(.value)"' "lastName davis"
  
  assert_json_eq '.events[2].http_client_response.status' "200"
}