#!/usr/bin/env bats

load '../../helper'

setup_file() {
  cd test/http_client/springboot
}

teardown_file() {
  stop_ws
}

@test "runs in spring boot jar" {
  run gradlew -q -PSPRING_BOOT_VERSION=$SPRING_BOOT_VERSION clean bootJar
  assert_success

  run java -javaagent:"$(find_agent_jar)" -jar build/libs/springboot-test.jar "$WS_URL"
  assert_success
}
