#!/usr/bin/env bats

load '../helper'

setup_file() {
  is_java 11 || skip "Java 11 is required"

  cd "${BATS_TEST_DIRNAME}" || exit 1
  mkdir -p "$LOG_DIR"

  export LOG="$LOG_DIR/gretty-tomcat.log"
  export SERVER_PORT=8080
  export WS_URL="http://localhost:${SERVER_PORT}/hello"
  
  _configure_logging

  echo -n "Starting gretty-tomcat test server..." >&3
  gradlew appStart -Pgretty.httpPort=${SERVER_PORT} &> $LOG &
  export WS_PID=$!

  wait_for_ws
}

teardown_file() {
  gradlew appStop || true
  # stop_ws might fail if /exit is not there, but it also waits for process to die.
  # We can try to just kill the gradle process if it's still running.
  kill "$WS_PID" || true
}

@test "hello world" {
  run _curl -sXGET "${WS_URL}"
  assert_success
  assert_output "Hello, World!"
}


