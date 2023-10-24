#!/usr/bin/env bats
#
# Runs smoke tests against a Spring sample application available here:
# https://github.com/spring-projects/spring-petclinic
#
# If running locally, keep in mind that this application will cache SQL results,
# likely causing subsequent test runs to fail.


load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'
load '../petclinic-shared/shared-setup.bash'

setup_file() {
  export AGENT_JAR="$(find_agent_jar)"

  export FIXTURE_DIR="test/junit4/spring-petclinic"
  rm -rf "${FIXTURE_DIR}"

  git clone build/fixtures/spring-petclinic "${FIXTURE_DIR}" >&3
  _shared_setup

  cp appmap.yml "${FIXTURE_DIR}"

  cd "${FIXTURE_DIR}"
  # Checkout the first commit before tests were upgraded top JUnit 5
  git checkout ce7c3f93 >&3
}

setup() {
  rm -rf tmp/appmap
}

run_tests() {
  local test="${@}"
  run ./mvnw -q -DtrimStackTrace=false \
    -Dcheckstyle.skip=true -Dspring-javaformat.skip=true \
    -DargLine="@{argLine} -javaagent:${AGENT_JAR}" \
    test -Dtest="$test"
}

@test "framework is captured" {
  run_tests "JUnit4Tests#testItPasses"
  assert_success
  output=$(< "tmp/appmap/junit/org_springframework_samples_petclinic_JUnit4Tests_testItPasses.appmap.json")

  assert_json_eq '.metadata.frameworks[0].name' 'JUnit'
  assert_json_eq '.metadata.frameworks[0].version' '4'
}

@test "defined_class is captured" {
  run_tests "JUnit4Tests#testItPasses"
  output=$(< "tmp/appmap/junit/org_springframework_samples_petclinic_JUnit4Tests_testItPasses.appmap.json")

  assert_json_eq '.metadata.recording.defined_class' 'org.springframework.samples.petclinic.JUnit4Tests'
}

@test "test_status set for passing test" {
  run_tests "JUnit4Tests#testItPasses"
  assert_success
  output=$(< "tmp/appmap/junit/org_springframework_samples_petclinic_JUnit4Tests_testItPasses.appmap.json")

  assert_json_eq '.metadata.test_status' "succeeded"
}

@test "test_status set for failed test" {
  run_tests "JUnit4Tests#testItFails"
  assert_failure

  output=$(< "tmp/appmap/junit/org_springframework_samples_petclinic_JUnit4Tests_testItFails.appmap.json")

  assert_json_eq '.metadata.test_status' "failed"
  assert_json_eq '.metadata.test_failure.message' 'false is not true'
  assert_json_eq '.metadata.test_failure.location' 'org/springframework/samples/petclinic/JUnit4Tests.java:19'
}