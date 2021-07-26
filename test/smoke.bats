#!/usr/bin/env bats
#
# Runs a smoke test against a Spring sample application available here:
# https://github.com/spring-projects/spring-petclinic
#
# If running locally, keep in mind that this application will cache SQL results,
# likely causing subsequent test runs to fail.

: ${WS_URL?}

load 'test_helper/bats-support/load'
load 'test_helper/bats-assert/load'
load 'helper'

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
  assert_json_eq '.enabled' 'true'
}

@test "successfully stop the current recording" {
  start_recording
  
  run _curl -sXDELETE "${WS_URL}/_appmap/record"

  assert_success

  assert_json '.classMap'
  assert_json '.events'
  assert_json '.version'
}

@test "recordings capture http request" {
  start_recording
  _curl -XGET "${WS_URL}"
  stop_recording

  assert_json '.events[] | .http_server_request'
}

# NB: Because of the way query results are cached in petclinic, this
# test will only pass the first time it's run.
@test "recordings capture sql queries" {
  start_recording
  _curl -XGET "${WS_URL}/vets.html"
  stop_recording

  assert_json '.events[] | .sql_query'
  assert_json '.events[] | .sql_query.database_type'
}

@test "records exceptions" {
  start_recording
  _curl -XGET "${WS_URL}/oups"
  stop_recording

  assert_json '.events[] | .exceptions'
}

@test "recordings have Java metadata" {
  start_recording
  _curl -XGET "${WS_URL}"
  stop_recording

  javac test/Props.java
  eval $(java test.Props java.vm.version java.vm.name)
  assert_json_eq '.metadata.language.name' 'java'
  assert_json_eq '.metadata.language.version' "${JAVA_VM_VERSION}"
  assert_json_eq '.metadata.language.engine' "${JAVA_VM_NAME}"
}

@test "message parameters contain path params from a Spring app" {
  start_recording
  _curl -XGET "${WS_URL}/owners/1/pets/1/edit"
  stop_recording

  assert_json_eq '.events[] | .http_server_request.normalized_path_info' '/owners/:ownerId/pets/:petId/edit'
  assert_json_contains '.events[] | .message' 'ownerId'
  assert_json_contains '.events[] | .message' 'petId'
}

@test "expected appmap captured" {
  start_recording
  
  # this route seems least likely to be affected by future changes
  _curl -XGET "${WS_URL}/oups"
  
  stop_recording

  # Sanity check the events and classmap
  assert_json_eq '.events | length' 6

  assert_json_eq '.classMap | length' 2
  assert_json_eq '[.classMap[0] | recurse | .name?] | join(".")' javax.servlet.http.HttpServlet.service
  assert_json_eq '[.classMap[1] | recurse | .name?] | join(".")' org.springframework.samples.petclinic.system.CrashController.triggerException
}

@test "expected number of http client events captured" {
  skip

  java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed 's/^1\.//'| cut -d'.' -f1 | cut -d'-' -f1)
  if [ $java_version -gt "8" ]; then
    skip "java version higher than 8."
  fi
  javac -g test/HttpClientTest.java
  java -Xbootclasspath/a:/appmap.jar -javaagent:/appmap.jar -Dappmap.debug -Dappmap.config.file=/appmap.yml \
   -Dappmap.output.directory=/tmp/appmap -Dappmap.record=test.HttpClientTest.main test.HttpClientTest ${WS_URL}
  output=$(</tmp/appmap/*.appmap.json)
  assert_json_eq '[.events[] | select(.http_client_request)] | length' 3
  assert_json_eq '[.events[] | select(.http_client_response)] | length' 3
}

# bats captures stdout and stderr to the same variable ($output). We
# need to hide the informational message from the agent commands so
# json assertions don't get confused.
@test "appmap agent init" {
  run bash -c 'java -jar /appmap.jar -d /spring-petclinic init 2>/dev/null'
  assert_success

  assert_json_contains '.configuration.contents' 'path: org.springframework.samples.petclinic'
}

@test "appmap agent status" {
  run bash -c 'java -jar /appmap.jar -d /spring-petclinic status 2>/dev/null'
  assert_success

  assert_json_eq '.properties.config.app' 'spring-petclinic'
  assert_json_eq '.properties.frameworks[0].name' 'gradle'
  assert_json_eq '.properties.frameworks[1].name' 'maven'
}
