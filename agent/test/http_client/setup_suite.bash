#!/usr/bin/env bats

load '../helper'
load '../petclinic-shared/shared-setup.bash'


setup_suite() {
  export FIXTURE_DIR="build/fixtures/spring-petclinic"
  _shared_setup
  start_petclinic >&3

    case "${JAVA_VERSION}" in
      1.8*)
        ;&
      11.*)
        SPRING_BOOT_VERSION="2.7.18"
        ;;
      *)
        SPRING_BOOT_VERSION="3.2.2"
        ;;
    esac
    export SPRING_BOOT_VERSION
}

teardown_suite() {
  stop_ws
}
