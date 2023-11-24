#!/usr/bin/env bash


_test_form_data_is_recorded_as_message_parameters() {
  start_recording
  run _curl -sf -o /dev/null "${WS_URL}/owners/1/edit" \
    --data-raw 'firstName=Ben&lastName=Franklin&address=110+W.+Liberty+St.&city=Madison&telephone=6085551023'
  assert_success
  local out="$BATS_TEST_TMPDIR/stop_recording_output"
  stop_recording "${out}"

  # run npx in a subshell so we can redirect stderr
  run bash -c "npx --yes @appland/appmap@latest prune --output-data \"${out}\" 2>/dev/null"
  assert_success

  run jq -r '[.events[] | select(.http_server_request.normalized_path_info == "/owners/{ownerId}/edit") | .message[] | .name] | join(",")' <<< "${output}"
  assert_output 'firstName,lastName,address,city,telephone'
}

_test_the_agent_doesnt_exhaust_theInputStream() {
  # Send a POST request with some data. If the agent incorrectly causes the body
  # to be consumed (e.g. by calling ServletRequest.getParameterMap), the number
  # of available bytes will be 0. 
  run _curl --data '{"some": "data"}' "${WS_URL}/showavailable"
  assert_success

  run jq -r '.available' <<< "${output}"
  assert [ $output -gt 0 ]

}