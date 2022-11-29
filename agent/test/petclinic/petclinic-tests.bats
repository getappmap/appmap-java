setup_file() {
  mkdir -p test/petclinic/classes
  javac -d test/petclinic/classes test/petclinic/Props.java
}

setup() {
  # bats doc says you'll get better error messages if you load helper scripts in
  # setup. (Note that loading them # in setup_file doesn't work.)
  load '../../build/bats/bats-support/load'
  load '../../build/bats/bats-assert/load'
  load '../helper'
  export AGENT_JAR="$(find_agent_jar)"


  cd build/fixtures/spring-petclinic
}

run_petclinic_test() {
  local cfg="${1:-appmap.yml}"

  run ./mvnw \
    -DargLine="@{argLine} -javaagent:${AGENT_JAR} -Dappmap.config.file=../../../test/petclinic/${cfg} -Dappmap.debug -Dappmap.debug.file=appmap.log" \
    test -Dtest="PetclinicIntegrationTests#testOwnerDetails"
  assert_success
}

@test "hooked functions are ordered correctly" {
  run_petclinic_test
  run cat ./tmp/appmap/junit/org_springframework_samples_petclinic_PetclinicIntegrationTests_testOwnerDetails.appmap.json
  assert_success

  assert_json_eq '.events[] | select(.id == 150) | .event' "call"
  assert_json_eq '.events[] | select(.id == 150) | .method_id' "testOwnerDetails"
  assert_json_eq '.events[] | select(.id == 168) | .parent_id' "150"
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

  run cat ./tmp/appmap/junit/org_springframework_samples_petclinic_PetclinicIntegrationTests_testOwnerDetails.appmap.json

  assert_json_eq '.classMap[0] | recurse(.children[]?) | select(.type? == "function" and .name? == "info").labels[0]' 'log'
}