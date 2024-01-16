#!/usr/bin/env bash

ROOT_DIR="$(dirname "${BASH_SOURCE[0]}")"

source "$ROOT_DIR/../helper.bash"
source "$ROOT_DIR/../bashunit-compat.sh"

set_up_before_script() {
  export AGENT_JAR="$(find_agent_jar)"
  export ANNOTATION_JAR="$(find_annotation_jar)"

  _configure_logging
}

set_up() {
  cd "$ROOT_DIR"
  rm -rf tmp/appmap
}

provider_framework() {
  echo ${TEST_FRAMEWORK:-junit testng}
}

run_tests() {
  local framework=$1
  local test="$2"
  ./gradlew -q clean test_${framework} --tests "$test"
}

# data_provider provider_framework
function test_metadata_captured_on_success() {
  local framework=$1
  local className="${framework^}"

  run_tests $framework "${className}Tests.testItPasses"
  assert_successful_code

  output="$(< tmp/appmap/${framework}/org_springframework_samples_petclinic_"${className}"Tests_testItPasses.appmap.json)"

  assert_json_eq '.metadata.test_status' "succeeded"

  assert_json_eq '.metadata.recorder.type' "tests"
  assert_json_eq '.metadata.recorder.name' "${framework}"

  assert_json_eq '.metadata.recording.defined_class' "org.springframework.samples.petclinic.${className}Tests"
}

# data_provider provider_framework
function test_test_status_set_for_failed_test() {
  local framework="$1"
  local className="${framework^}"
  run_tests $framework "${className}Tests.testItFails"
  assert_general_error

  output="$(< tmp/appmap/${framework}/org_springframework_samples_petclinic_${className}Tests_testItFails.appmap.json)"

  assert_json_eq '.metadata.test_status' "failed"
  assert_json_contains '.metadata.test_failure.message' 'false is not true'
  assert_json_eq '.metadata.test_failure.location' "src/test/java/org/springframework/samples/petclinic/${className}Tests.java:20"
}

# data_provider provider_framework
function test_NoAppMap_on_method_disables_recording() {
  local framework="$1"
  local className="${framework^}"

  output="$(run_tests $framework "${className}Tests.testAnnotatedMethodNotRecorded")"
  assert_successful_code
  assert_contains "$output" "passing annotated test, not recorded"

  assert_file_not_exists tmp/appmap/${framework}/org_springframework_samples_petclinic_${className}Tests_testItsNotRecorded.appmap.json
}

# data_provider provider_framework
function test_NoAppMap_on_class_disables_recording() {
  local framework="$1"
  local className="${framework^}"

  output="$(run_tests $framework "${className}Tests\$TestClass.testAnnotatedClassNotRecorded")"
  assert_successful_code
  assert_contains "$output" "passing annotated class, not recorded"

  assert_file_not_exists tmp/appmap/${framework}/org_springframework_samples_petclinic_${className}Tests_TestClass_testAnnotatedClassNotRecorded.appmap.json
}

function test_TestNG_expected_exception() {
  run_tests testng "TestngTests.testItThrows"
  assert_successful_code $? 'tests failed'

  output="$(< tmp/appmap/testng/org_springframework_samples_petclinic_TestngTests_testItThrows.appmap.json)"

  assert_json_eq '.metadata.test_status' "succeeded"
}
