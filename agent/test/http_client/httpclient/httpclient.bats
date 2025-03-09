#!/usr/bin/env bats

load '../../helper'
load '../../petclinic-shared/shared-setup.bash'


setup_file() {
  cd test/http_client/httpclient
}

@test "request without query" {
  run ./gradlew -q -PmainClass=httpclient.HttpClientTest run ${DEBUG} --args "${WS_URL}/vets"

  assert_json_eq '.events[1].http_client_request.request_method' "GET"
  assert_json_eq '.events[1].http_client_request.url' "${WS_URL}/vets"

  # Neither message nor parameters should be set
  assert_json_eq '.events[1].message' ''
  assert_json_eq '.events[1].parameters' ''
}

@test "request with query" {
  run ./gradlew -q -PmainClass=httpclient.HttpClientTest run ${DEBUG} --args "${WS_URL}/owners?lastName=davis"

  assert_json_eq '.events[1].http_client_request.url' "${WS_URL}/owners"
  assert_json_eq '.events[1].message | length' 1
  assert_json_eq '.events[1].message[0] | "\(.name) \(.value)"' "lastName davis"

  assert_json_eq '.events[2].http_client_response.status' "200"
}

@test "request without Content-Type" {
  run ./gradlew -q -PmainClass=httpclient.HttpClientTest run ${DEBUG} --args "${WS_URL}/no-content"

  assert_json_eq '.events[1].http_client_request.url' "${WS_URL}/no-content"
  assert_json_eq '.events[2].http_client_response.status' "200"
}

@test "request with HttpHost" {
  run ./gradlew -q -PmainClass=httpclient.HttpHostTest run ${DEBUG} --args "${WS_HOST} ${WS_PORT} /owners?lastName=davis"

  assert_json_eq '.events[1].http_client_request.url' "${WS_URL}/owners"
  assert_json_eq '.events[1].message | length' 1
  assert_json_eq '.events[1].message[0] | "\(.name) \(.value)"' "lastName davis"

  assert_json_eq '.events[2].http_client_response.status' "200"
}