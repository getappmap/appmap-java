#!/usr/bin/env bash

_test_requests_for_nonstatic_resources_are_recorded_by_default() {
  run _curl -XGET "${WS_URL}/owners/1/pets/1/edit"
  assert_success 
  local dir="${FIXTURE_DIR}/tmp/appmap/request_recording"
  
  wait_for_glob "${dir}/*owners_1_pets_1_edit.appmap.json"
  run bash -c "compgen -G ${dir}/*owners_1_pets_1_edit.appmap.json"
  assert_success

  output="$(<${output})"
  assert_json_eq '.events[] | .http_server_request | .path_info' '/owners/1/pets/1/edit' 
  assert_json_eq '.metadata.recorder.type' 'requests'
}

_test_request_for_static_resources_dont_generate_recordings() {
  run _curl -XGET "${WS_URL}/resources/css/petclinic.css"
  assert_success

  # After we've made the request that shouldn't generate a recording, make
  # another that should.
  run _curl -XGET "${WS_URL}/owners/1/pets/1/edit"
  assert_success 
  local dir="${FIXTURE_DIR}/tmp/appmap/request_recording"
  
  wait_for_glob "${dir}/*owners_1_pets_1_edit.appmap.json"
  run bash -c "compgen -G ${dir}/*owners_1_pets_1_edit.appmap.json"
  assert_success

  # Now that that's done, make sure there's no recording for the static
  # resource. compen -G returns 1 if asked to find a glob that has no matches, 2
  # if it's invoked incorrectly.
  run bash -c "compgen -G ${dir}/*resources_css_petclinic_css.appmap.json"
  assert_failure 1
}