#!/usr/bin/env bats
#
# Runs smoke tests against a Spring sample application available here:
# https://github.com/spring-projects/spring-petclinic
#
# If running locally, keep in mind that this application will cache SQL results,
# likely causing subsequent test runs to fail.

WS_URL=${WS_URL:-http://localhost:8080}

load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'

large_test_file="test/junit/spring-petclinic/target/appmap/org_springframework_samples_petclinic_owner_PetControllerTests_testProcessCreationFormHasErrors.appmap.json"

@test "junit tests produce AppMaps" {
  assert [ -d test/junit/spring-petclinic/target/appmap ]
}

@test "framework is captured" {
  output=$(cat "$large_test_file")

  assert_json_eq '.metadata.framework.name' 'junit'
}

@test "defined_class is captured" {
  output=$(cat "$large_test_file")

  assert_json_eq '.metadata.recording.defined_class' 'org.springframework.samples.petclinic.owner.PetControllerTests'
}

# TODO: Verify source_location, test_status, exception
# To do that, the default location of the appmap.jar that's injected by maven needs to be changed.
# The default will look something like this:
# -javaagent:/Users/kgilpin/.m2/repository/com/appland/appmap-agent/1.0.4/appmap-agent-1.0.4.jar=com.appland.appmap.LoadJavaAppMapAgentMojo@53a20aab
# We need it to point to the freshly built appmap.jar.
