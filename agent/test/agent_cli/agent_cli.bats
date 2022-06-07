#!/usr/bin/env bats

load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'

appmap_jar=build/libs/$(ls build/libs | grep 'appmap-[[:digit:]]')

# bats captures stdout and stderr to the same variable ($output). We
# need to hide the informational message from the agent commands so
# json assertions don't get confused.
@test "appmap agent init packages" {
  run bash -c "java -jar $appmap_jar -d test/agent_cli/sampleproj init 2>/dev/null"
  assert_success
  assert_json_contains '.configuration.contents' 'path: pkg1'
  assert_json_contains '.configuration.contents' 'path: pkg2'
}

@test "appmap agent init empty project" {
  run bash -c "java -jar $appmap_jar -d test/agent_cli/emptyproj init 2>/dev/null"
  assert_success

  assert_json_contains '.configuration.contents' 'path: com.mycorp.pkg'
}

@test "appmap agent status petclinic" {
  run bash -c "java -jar $appmap_jar -d test/agent_cli/spring-petclinic status 2>/dev/null"
  assert_success

  assert_json_eq '.properties.config.app' 'spring-petclinic'

  assert_json_eq '.properties.config.present' 'true'
  assert_json_eq '.properties.config.valid' 'true'
  assert_json_eq '.properties.frameworks[0].name' 'gradle'
  assert_json_eq '.properties.frameworks[1].name' 'maven'

  # TODO: Configure the agent in the spring-petclinic pom.xml and verify that it's
  # reported present and valid by the agent status command.
}

@test "appmap agent validate" {
  run bash -c "java -jar $appmap_jar -d test/agent_cli/spring-petclinic validate 2>/dev/null"
  assert_success

  # Shouldn't be any errors
  assert_json '.errors | length == 0'

  # Sanity check a couple of the config schema properties
  assert_json_eq '.schema.type' 'object'
  assert_json_eq '.schema.additionalProperties' 'true'
  assert_json_eq '.schema.required' '[
  "name"
]'
}
