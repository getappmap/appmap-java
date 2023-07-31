#!/usr/bin/env bats
#
# Runs smoke tests against a Spring sample application available here:
# https://github.com/spring-projects/spring-petclinic
#
# If running locally, keep in mind that this application will cache SQL results,
# likely causing subsequent test runs to fail.

load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'

setup_file() {
  start_petclinic -Dappmap.recording.requests=false >&3

  rm -rf build/fixtures/spring-petclinic/tmp
}

teardown_file() {
  stop_ws
}

@test "the user can disable request recording" {
  run _curl -XGET "${WS_URL}/owners/1/pets/1/edit"
  assert_success 
  local dir='build/fixtures/spring-petclinic'

  # sanity check
  assert [ -d ${dir}/target ]
  
  refute [ -f ${dir}/tmp/appmap/request_recording/*owners_1_pets_1_edit.appmap.json ]
}
