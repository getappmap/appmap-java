#!/usr/bin/env bats

load ../helper

setup_file() {
  export AGENT_JAR="$(find_agent_jar)"
  export ANNOTATION_JAR="$(find_annotation_jar)"
  _configure_logging
}

setup() {
  cd "$(dirname "$BATS_TEST_FILENAME")"
  rm -rf tmp/appmap
}

# Helper function to run tests
run_framework_test() {
  local framework=$1
  local test="$2"
  run ./gradlew cleanTest test_${framework} --tests "$test"
}

@test "metadata captured on success for junit" {
  run_framework_test "junit" "JunitTests.testItPasses"
  assert_success
  
  output="$(< tmp/appmap/junit/org_springframework_samples_petclinic_JunitTests_testItPasses.appmap.json)"
  
  assert_json_eq '.metadata.test_status' "succeeded"
  assert_json_eq '.metadata.recorder.type' "tests"
  assert_json_eq '.metadata.recorder.name' "junit"
  assert_json_eq '.metadata.recording.defined_class' "org.springframework.samples.petclinic.JunitTests"
}

@test "metadata captured on success for testng" {
  run_framework_test "testng" "TestngTests.testItPasses"
  assert_success
  
  output="$(< tmp/appmap/testng/org_springframework_samples_petclinic_TestngTests_testItPasses.appmap.json)"
  
  assert_json_eq '.metadata.test_status' "succeeded"
  assert_json_eq '.metadata.recorder.type' "tests"
  assert_json_eq '.metadata.recorder.name' "testng"
  assert_json_eq '.metadata.recording.defined_class' "org.springframework.samples.petclinic.TestngTests"
}

@test "test status set for failed test in junit" {
  run_framework_test "junit" "JunitTests.testItFails"
  assert_failure
  
  output="$(< tmp/appmap/junit/org_springframework_samples_petclinic_JunitTests_testItFails.appmap.json)"
  
  assert_json_eq '.metadata.test_status' "failed"
  assert_json_contains '.metadata.test_failure.message' 'false is not true'
  assert_json_eq '.metadata.test_failure.location' "src/test/java/org/springframework/samples/petclinic/JunitTests.java:23"
}

@test "test status set for failed test in testng" {
  run_framework_test "testng" "TestngTests.testItFails"
  assert_failure
  
  output="$(< tmp/appmap/testng/org_springframework_samples_petclinic_TestngTests_testItFails.appmap.json)"
  
  assert_json_eq '.metadata.test_status' "failed"
  assert_json_contains '.metadata.test_failure.message' 'false is not true'
  assert_json_eq '.metadata.test_failure.location' "src/test/java/org/springframework/samples/petclinic/TestngTests.java:20"
}

@test "NoAppMap on method disables recording for junit" {
  run_framework_test "junit" "JunitTests.testAnnotatedMethodNotRecorded"
  assert_success
  assert_output --partial "passing annotated test, not recorded"
  
  [ ! -f tmp/appmap/junit/org_springframework_samples_petclinic_JunitTests_testItsNotRecorded.appmap.json ]
}

@test "NoAppMap on method disables recording for testng" {
  run_framework_test "testng" "TestngTests.testAnnotatedMethodNotRecorded"
  assert_success
  assert_output --partial "passing annotated test, not recorded"
  
  [ ! -f tmp/appmap/testng/org_springframework_samples_petclinic_TestngTests_testItsNotRecorded.appmap.json ]
}

@test "NoAppMap on class disables recording for junit" {
  run_framework_test "junit" "JunitTests\$TestClass.testAnnotatedClassNotRecorded"
  assert_success
  assert_output --partial "passing annotated class, not recorded"
  
  [ ! -f tmp/appmap/junit/org_springframework_samples_petclinic_JunitTests_TestClass_testAnnotatedClassNotRecorded.appmap.json ]
}

@test "NoAppMap on class disables recording for testng" {
  run_framework_test "testng" "TestngTests\$TestClass.testAnnotatedClassNotRecorded"
  assert_success
  assert_output --partial "passing annotated class, not recorded"
  
  [ ! -f tmp/appmap/testng/org_springframework_samples_petclinic_TestngTests_TestClass_testAnnotatedClassNotRecorded.appmap.json ]
}

@test "TestNG expected exception" {
  run_framework_test "testng" "TestngTests.testItThrows"
  assert_success
  
  output="$(< tmp/appmap/testng/org_springframework_samples_petclinic_TestngTests_testItThrows.appmap.json)"
  assert_json_eq '.metadata.test_status' "succeeded"
}

@test "No InternalError on different thread exception" {
  run_framework_test "junit" "JunitTests.offThreadExceptionTest"
  assert_failure
  # The test should fail with a RuntimeException, but not an InternalError
  assert_output --partial "java.lang.RuntimeException"
  refute_output --partial "java.lang.InternalError"
}