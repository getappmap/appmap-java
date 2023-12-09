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

setup_file() {
  export AGENT_JAR="$(find_agent_jar)"
  export ANNOTATION_JAR="$(find_annotation_jar)"

  cd test/junit4
  _configure_logging
}

setup() {
  rm -rf tmp/appmap
}

run_tests() {
  run ./gradlew clean test -PagentJar="$AGENT_JAR" -PannotationJar="$ANNOTATION_JAR" --tests "$@"
}

@test "framework is captured" {
  run_tests "JUnit4Tests.testItPasses"
  assert_success

  run cat tmp/appmap/junit/org_springframework_samples_petclinic_JUnit4Tests_testItPasses.appmap.json
  assert_success

  assert_json_eq '.metadata.frameworks[0].name' 'JUnit'
  assert_json_eq '.metadata.frameworks[0].version' '4'
}

@test "defined_class is captured" {
  run_tests "JUnit4Tests.testItPasses"
  assert_success

  run cat tmp/appmap/junit/org_springframework_samples_petclinic_JUnit4Tests_testItPasses.appmap.json
  assert_success

  assert_json_eq '.metadata.recording.defined_class' 'org.springframework.samples.petclinic.JUnit4Tests'
}

@test "test_status set for passing test" {
  run_tests "JUnit4Tests.testItPasses"
  assert_success
  run cat tmp/appmap/junit/org_springframework_samples_petclinic_JUnit4Tests_testItPasses.appmap.json
  assert_success

  assert_json_eq '.metadata.test_status' "succeeded"
}

@test "test_status set for failed test" {
  run_tests "JUnit4Tests.testItFails"
  assert_failure

  run cat tmp/appmap/junit/org_springframework_samples_petclinic_JUnit4Tests_testItFails.appmap.json
  assert_success

  assert_json_eq '.metadata.test_status' "failed"
  assert_json_eq '.metadata.test_failure.message' 'false is not true'
  assert_json_eq '.metadata.test_failure.location' 'src/test/java/org/springframework/samples/petclinic/JUnit4Tests.java:20'
}

@test "NoAppMap on method disables recording" {
  run_tests "JUnit4Tests.testAnnotatedMethodNotRecorded"
  assert_output --partial "passing annotated test, not recorded"

  run test \! -f tmp/appmap/junit/org_springframework_samples_petclinic_JUnit4Tests_testItsNotRecorded.appmap.json
  assert_success
}

@test "NoAppMap on class disables recording" {
  run_tests 'JUnit4Tests$TestClass.testAnnotatedClassNotRecorded'
  assert_output --partial "passing annotated class, not recorded"

  run test \! -f tmp/appmap/junit/org_springframework_samples_petclinic_JUnit4Tests_TestClass_testAnnotatedClassNotRecorded.appmap.json
  assert_success
}