#!/usr/bin/env bash


_test_form_data_is_recorded_as_message_parameters() {
  start_recording
  run _curl -sf -o /dev/null "${WS_URL}/owners/1/edit" \
    --data-raw 'firstName=Ben&lastName=Franklin&address=110+W.+Liberty+St.&city=Madison&telephone=6085551023'
  assert_success
  stop_recording

  local appmap="$(npx @appland/appmap prune --output-data /dev/stdin <<< "$output")"
  run jq -r '.events[] | select(.http_server_request.normalized_path_info == "/owners/{ownerId}/edit") | .message[] | .name' <<< "${appmap}"
  assert_output 'firstName
lastName
address
city
telephone'
}

_test_the_agent_doesnt_exhaust_theInputStream() {
  # Send a POST request with some data. If the agent incorrectly causes the body
  # to be consumed (e.g. by calling ServletRequest.getParameterMap), the number
  # of available bytes will be 0. 
  run _curl --data '{"some": "data"}' "${WS_URL}/showavailable"
  assert_success

  assert_json_eq ".available" "31"
}