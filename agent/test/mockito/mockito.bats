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

# The tests have to pass with no agent attached, otherwise a failure under the
# agent doesn't tell us anything.
@test "control: mockito tests pass with no agent" {
  run gradlew cleanTest test_noagent
  assert_success
}

# Recording a mock used to call toString() on it, which Mockito counts as an
# invocation. That stole pending argument matchers ("2 matchers expected, 1
# recorded") and silently broke stubbing, so stubbed calls returned the Java
# type default.
@test "recording does not disturb mockito" {
  run gradlew cleanTest test_appmap
  assert_success
  refute_output --partial "InvalidUseOfMatchersException"
}

# The flip side of the fix: a mock's value is a placeholder, because asking the
# mock for its value is what caused the trouble.
@test "a mock is recorded as a placeholder, not by calling it" {
  run gradlew cleanTest test_appmap --tests '*MockRecordingTest.testRecordingAMockAddsNoInteractions'
  assert_success

  output="$(< tmp/appmap/junit/com_example_mockito_MockRecordingTest_testRecordingAMockAddsNoInteractions.appmap.json)"

  # The mock really is recorded as a parameter, so this isn't vacuous...
  assert_json_contains \
    '[.events[] | select(.method_id=="identify") | .parameters[0].class] | first' 'MockitoMock'
  # ...and its value came from us, not from the mock.
  assert_json_eq \
    '[.events[] | select(.method_id=="identify") | .parameters[0].value] | first' '[mocked]'
}
