load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'
load '../petclinic-shared/shared-setup.bash'


setup_file() {
  mkdir -p test/petclinic/classes
  javac -d test/petclinic/classes test/petclinic/Props.java

  # When PetClinic updated to Spring Boot 3, they changed the name of the
  # integration tests from PetclinicIntegrationTests to
  # PetClinicIntegrationTests. The tests here need to support both, so use a
  # pattern that matches them.
  export TEST_NAME="Pet?linicIntegrationTests"
  export FIXTURE_DIR="build/fixtures/spring-petclinic"
  _shared_setup

  export AGENT_JAR="$(find_agent_jar)"
}

setup() {
  cd build/fixtures/spring-petclinic
  rm -rf tmp/appmap
}


run_petclinic_test() {
  local cfg="${1:-appmap.yml}"

  run ./mvnw \
    -Dcheckstyle.skip=true -Dspring-javaformat.skip=true \
    -DargLine="@{argLine} -javaagent:${AGENT_JAR} -Dappmap.config.file=../../../test/petclinic/${cfg}" \
    test -Dtest="${TEST_NAME}"
  assert_success
}

@test "hooked functions are ordered correctly" {
  run_petclinic_test
  run cat ./tmp/appmap/junit/org_springframework_samples_petclinic_${TEST_NAME}_testOwnerDetails.appmap.json
  assert_success

  # Thread 1 is the test runner's main thread. Check to make sure that parent_id
  # of the "return" event matches the id of the "call" event.
  assert_json_eq '.events | map(select(.thread_id == 1)) | ((.[0].event == "call" and .[1].event == "return") and (.[1].parent_id == .[0].id))' "true"
}

@test "extra properties in appmap.yml are ignored when config is loaded" {
  # Don't need to execute the tests, just running with the agent loads the
  # config
  run java -cp ../../../test/petclinic/classes \
    -javaagent:${AGENT_JAR} -Dappmap.config.file=../../../test/petclinic/appmap-extra.yml \
    petclinic.Props
  assert_success
}

@test "log methods are labeled" {
  run_petclinic_test appmap-labels.yml

  run cat ./tmp/appmap/junit/org_springframework_samples_petclinic_${TEST_NAME}_testOwnerDetails.appmap.json

  assert_json_eq '.classMap[0] | recurse(.children[]?) | select(.type? == "function" and .name? == "info").labels[0]' 'log'
}

@test "test_status set for successful test" {
  run ./mvnw \
    -Dcheckstyle.skip=true -Dspring-javaformat.skip=true \
    -DargLine="@{argLine} -javaagent:${AGENT_JAR} -Dappmap.config.file=../../../test/petclinic/appmap.yml" \
    test -Dtest="JUnit5Tests#testItPasses"
  assert_success

  run cat ./tmp/appmap/junit/org_springframework_samples_petclinic_JUnit5Tests_testItPasses.appmap.json
  assert_success

  assert_json_eq '.metadata.frameworks[0].name' 'JUnit'
  assert_json_eq '.metadata.frameworks[0].version' '5'

  assert_json_eq '.metadata.test_status' 'succeeded'
}

@test "test_status set for failed test" {
  run ./mvnw \
    -Dcheckstyle.skip=true -Dspring-javaformat.skip=true \
    -DargLine="@{argLine} -javaagent:${AGENT_JAR} -Dappmap.config.file=../../../test/petclinic/appmap.yml" \
    test -Dtest="JUnit5Tests#testItFails"
  assert_failure

  run cat ./tmp/appmap/junit/org_springframework_samples_petclinic_JUnit5Tests_testItFails.appmap.json
  assert_success

  assert_json_eq '.metadata.test_status' 'failed'
  assert_json_eq '.metadata.test_failure.message' 'expected: <true> but was: <false>'
  assert_json_eq '.metadata.test_failure.location' 'org/springframework/samples/petclinic/JUnit5Tests.java:19'
}  

