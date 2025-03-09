#!/usr/bin/env bats
#
# Runs smoke tests against a Spring sample application available here:
# https://github.com/spring-projects/spring-petclinic
#
# If running locally, keep in mind that this application will cache SQL results,
# likely causing subsequent test runs to fail.

load '../helper'
load '../petclinic-shared/shared-setup.bash'

setup_file() {
  export FIXTURE_DIR="build/fixtures/spring-petclinic"
  _shared_setup

  start_petclinic -Dappmap.recording.requests=false >&3

}

teardown_file() {
  stop_ws
}

setup() {
  rm -rf "${FIXTURE_DIR}/target/tmp/appmap"
}

@test "the user can disable request recording" {
  run _curl -XGET "${WS_URL}/owners/1/pets/1/edit"
  assert_success 
  local dir="${FIXTURE_DIR}/target"

  # sanity check
  assert [ -d ${dir} ]
  
  refute [ -f ${dir}/tmp/appmap/request_recording/*owners_1_pets_1_edit.appmap.json ]
}
