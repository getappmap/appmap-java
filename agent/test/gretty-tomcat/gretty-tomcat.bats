#!/usr/bin/env bats

load '../helper'

setup_file() {
  _require_java_version 11

  mkdir -p build/log

  export LOG="$(getcwd)/build/log/gretty-tomcat.log"
  export SERVER_PORT=8080
  export WS_URL="http://localhost:${SERVER_PORT}"
  
  cd ${BATS_TEST_DIRNAME}
  _configure_logging

  ./gradlew appStart -Pgretty.httpPort=${SERVER_PORT} &> $LOG &
  export JVM_MAIN_CLASS=org.gradle.wrapper.GradleWrapperMain

  wait_for_ws "${WS_URL}/hello"
}

teardown_file() {
  ./gradlew appStop || true
  # stop_ws might fail if /exit is not there, but it also waits for process to die.
  # We can try to just kill the gradle process if it's still running.
  pkill -P $$ -f "GradleWrapperMain" || true
}

@test "hello world" {
  run _curl -sXGET "${WS_URL}/hello"
  assert_success
  assert_output "Hello, World!"
}


