#!/usr/bin/env bats
#
# Test Apache Http Core.
# Create a reverse proxy to the petclinic server. Check to make sure each of the remote-recording endpoints
# return the appropriate response.


load '../helper'

setup_file() {
mkdir -p build/log

export LOG="$(getcwd)/build/log/httpcore.log"
export SERVER_PORT=9090
export WS_URL=${WS_URL:-http://localhost:9090}
  cd test/httpcore
  _configure_logging

  printf 'getting set up' >&3
  ./gradlew run --args "${SERVER_PORT}"   &> $LOG &
  export JVM_MAIN_CLASS=org.gradle.wrapper.GradleWrapperMain

  while [ -z "$(curl -sI ${WS_URL} | grep 'HTTP/1.1 200')" ]; do
    if ! jcmd $JVM_MAIN_CLASS VM.uptime >&/dev/null; then
      echo "$JVM_MAIN_CLASS failed" >&3
      cat $LOG >&3
      exit 1
    fi
    printf ' .' >&3
    sleep 1
  done
  printf ' ok\n\n' >&3
}

teardown_file() {
  stop_ws
}

@test "the recording status reports disabled when not recording" {
  run _curl -sXGET "${WS_URL}/_appmap/record"
  assert_success

  assert_json_eq '.enabled' 'false'
}

@test "successfully start a new recording" {
  run _curl -sIXPOST "${WS_URL}/_appmap/record"
  assert_success

  echo "${output}" \
    | grep "HTTP/1.1 200"
}

@test "grab a checkpoint during remote recording" {
  start_recording

  _curl -XGET "${WS_URL}"

  run _curl -sXGET "${WS_URL}/_appmap/record/checkpoint"
  assert_success

  assert_json '.classMap'
  assert_json '.events'
  assert_json '.version'

  run _curl -sXGET "${WS_URL}/_appmap/record"
  assert_success
  assert_json_eq '.enabled' 'true'

  run _curl -sXDELETE "${WS_URL}/_appmap/record"
  assert_success

  assert_json '.classMap'
  assert_json '.events'
  assert_json '.version'
}

@test "successfully stop the current recording" {
  start_recording

  _curl -XGET "${WS_URL}"
  run _curl -sXDELETE "${WS_URL}/_appmap/record"
  assert_success

  assert_json '.classMap'
  assert_json '.events'
  assert_json '.version'
}

@test "expected appmap captured" {
  start_recording

  _curl -XGET "${WS_URL}"

  stop_recording

  # Sanity check the events and classmap
  assert_json_eq '.events | length' 6

  # Make sure the return from the request handler and the http_server_response are ordered properly
  assert_json_eq '.events[4].parent_id' 47
  assert_json_eq '.events[5].event' return
  assert_json_eq '.events[5].http_server_response.status' 200

  assert_json_eq '.classMap | length' 1

  # Pick the functions out of the classMap
  local filter='.classMap | paths(objects) as $p | getpath($p) | select(.type == "function")'
  local join='join(",")'

  assert_json_eq "${filter} | select(.name == \"sayHello\").labels | ${join}" 'say,Hello'
  assert_json_eq "${filter} | select(.name == \"handle\").labels | ${join}" 'handler'
}
