setup() {
  # bats doc says you'll get better error messages if you load helper scripts in
  # setup. (Note that loading them # in setup_file doesn't work.)
  load '../../build/bats/bats-support/load'
  load '../../build/bats/bats-assert/load'
  load '../helper'
  export AGENT_JAR="$(find_agent_jar)"


  cd build/fixtures/spring-petclinic
}

@test "hooked functions are ordered correctly" {
  run ./mvnw \
    -DargLine="@{argLine} -javaagent:${AGENT_JAR} -Dappmap.config.file=../../../test/petclinic/appmap.yml"  \
    test -Dtest="PetclinicIntegrationTests#testOwnerDetails"
  assert_success

  run cat ./tmp/appmap/org_springframework_samples_petclinic_PetclinicIntegrationTests_testOwnerDetails.appmap.json
  assert_success

  assert_json_eq '.events[] | select(.id == 150) | .event' "call"
  assert_json_eq '.events[] | select(.id == 150) | .method_id' "testOwnerDetails"
  assert_json_eq '.events[] | select(.id == 168) | .parent_id' "150"
}
