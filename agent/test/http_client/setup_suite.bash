#!/usr/bin/env bats

load '../helper'
load '../petclinic-shared/shared-setup.bash'


setup_suite() {
  export FIXTURE_DIR="build/fixtures/spring-petclinic"
  _shared_setup
  start_petclinic >&3
}

teardown_suite() {
  stop_ws
}
