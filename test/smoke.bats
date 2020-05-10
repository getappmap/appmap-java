#!/usr/bin/env bats

load 'test_helper/bats-support/load'
load 'test_helper/bats-assert/load'

_curl() {
  curl -H 'Accept: application/json' $@
}

start_recording() {
  _curl -sXPOST "${WS_URL}/_appmap/record"
}

stop_recording() {
  output="$(_curl -sXDELETE ${WS_URL}/_appmap/record | tee /dev/stderr)"
}

teardown() {
  stop_recording
}

assert_json_contains() {
  : "${output?}"

  # Expect a jq query as $1. Pipe it into `select`, so null values
  # will show up as a empty strings, rather than "null". (See
  # discussion here: https://github.com/stedolan/jq/issues/24)
  local query="${1?} | select(. == null | not)"

  [[ -v DEBUG_JSON ]] && jq <<< "${output}" >&3
  local result=$(jq -r "${query}" <<< "${output}")

  [[ -v DEBUG_JSON ]] && echo "result: ${result}" >&3
  if [[ -v 2 ]]; then
    assert [ "${result}" == "$2" ]
  else
    refute [ -z "${result}" ]
  fi
}

@test "the recording status reports disabled when not recording" {
  run _curl -sXGET "${WS_URL}/_appmap/record"

  assert_success

  assert_json_contains .enabled false
}

@test "successfully start a new recording" {
  run _curl -sIXPOST "${WS_URL}/_appmap/record"

  assert_success
  
  echo "${output}" \
    | grep "HTTP/1.1 200"
}

@test "fail to start a recording while recording is already in progress" {
  start_recording
  
  run _curl -sIXPOST "${WS_URL}/_appmap/record"

  assert_success

  echo "${output}" \
    | grep "HTTP/1.1 409"
}

@test "the recording status reports enabled when recording" {
  start_recording
  
  run _curl -sXGET "${WS_URL}/_appmap/record"

  assert_success

  echo "${output}" \
    | jq .enabled \
    | grep true
}

@test "successfully stop the current recording" {
  start_recording
  
  run _curl -sXDELETE "${WS_URL}/_appmap/record"

  assert_success

  assert_json_contains .classMap
  assert_json_contains .events
  assert_json_contains .version
}

@test "recordings capture http request" {
  start_recording
  _curl -XGET "${WS_URL}"
  stop_recording

  assert_json_contains '.events[] | .http_server_request'
}

# NB: Because of the way query results are cached in petclinic, this
# test will only pass the first time it's run.
@test "recordings capture sql queries" {
  start_recording
  _curl -XGET "${WS_URL}/vets.html"
  stop_recording

  assert_json_contains '.events[] | .sql_query'
}

@test "records exceptions" {
  start_recording
  _curl -XGET "${WS_URL}/oups"
  stop_recording

  assert_json_contains '.events[] | .exceptions'
}
