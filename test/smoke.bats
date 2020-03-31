#!/usr/bin/env bats

start_recording() {
  curl -sXPOST "${WS_URL}/_appmap/record"
}

stop_recording() {
  output="$(curl -sXDELETE ${WS_URL}/_appmap/record)"
}

assert_output() {
  read input
  [ "${input}" != "" ]
}

@test "the recording status reports disabled when not recording" {
  run curl -sXGET "${WS_URL}/_appmap/record"

  [ "${status}" -eq 0 ]

  echo "${output}" \
    | jq .enabled \
    | grep false
}

@test "successfully start a new recording" {
  run curl -sIXPOST "${WS_URL}/_appmap/record"

  [ "${status}" -eq 0 ]
  echo "${output}" \
    | grep "HTTP/1.1 200"
}

@test "fail to start a recording while recording is already in progress" {
  run curl -sIXPOST "${WS_URL}/_appmap/record"

  [ "${status}" -eq 0 ]

  echo "${output}" \
    | grep "HTTP/1.1 409"
}

@test "the recording status reports enabled when recording" {
  run curl -sXGET "${WS_URL}/_appmap/record"

  [ "${status}" -eq 0 ]

  echo "${output}" \
    | jq .enabled \
    | grep true
}

@test "successfully stop the current recording" {
  run curl -sXDELETE "${WS_URL}/_appmap/record"

  [ "${status}" -eq 0 ]

  echo "${output}" \
    | jq -r .classMap \
    | assert_output

  echo "${output}" \
    | jq -r .events \
    | assert_output

  echo "${output}" \
    | jq -r .version \
    | assert_output
}

@test "recordings capture http requests" {
  start_recording
  curl -XGET "${WS_URL}"
  stop_recording

  echo "${output}" \
    | jq '.events[] | .http_server_request' \
    | assert_output
}

@test "recordings capture sql queries" {
  start_recording
  curl -XGET "${WS_URL}/vets.html"
  stop_recording

  echo "${output}" \
    | jq '.events[] | .sql_query' \
    | assert_output
}

@test "records exceptions" {
  start_recording
  curl -XGET "${WS_URL}/oups"
  stop_recording

  echo "${output}" \
    | jq '.events[] | select(.event=="exception")' \
    | assert_output
}
