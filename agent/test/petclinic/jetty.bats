load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'

load '../petclinic-shared/shared-setup.bash'
load '../petclinic-shared/static-resources.bash'

setup_file() {
  export FIXTURE_DIR="build/fixtures/spring-petclinic"
  _shared_setup

  export MAVEN_PROFILE=-Pjetty
  start_petclinic >&3
}

teardown_file() {
  stop_ws
}

@test "requests for non-static resources are recorded by default" {
  _test_requests_for_nonstatic_resources_are_recorded_by_default
}
