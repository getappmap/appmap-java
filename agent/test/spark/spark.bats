load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'

recording_dir="app/build/tmp/appmap/request_recording"

setup_file() {
  export WS_SCHEME="http" WS_HOST="localhost" WS_PORT="8080"
  export WS_URL="${WS_SCHEME}://${WS_HOST}:${WS_PORT}"
  
  export AGENT_JAR="$(find_agent_jar)"

  cd test/spark
  ./gradlew -q -PappmapJar="${AGENT_JAR}" run &

  export JVM_PID=$!

  wait_for_ws
}

teardown_file() {
  stop_ws
}

setup() {
  rm -rf "$recording_dir"
}

@test "Spark remote recording works" {
  run _curl -sXGET "${WS_URL}/_appmap/record"
  assert_success
  assert_json_eq '.enabled' 'false'

  run _curl -sXPOST "${WS_URL}/_appmap/record"
  assert_success

  _curl -XGET "${WS_URL}"

  run _curl -sXDELETE "${WS_URL}/_appmap/record"
  assert_success
  assert_json '.classMap'
  assert_json '.events'
  assert_json '.version'
}


@test "Spark request recording works" {
  _curl -sXGET "${WS_URL}"

  # Give the server a chance to write the request recording
  wait_for_glob "$recording_dir/*.appmap.json"

  run cat "$recording_dir"/*.appmap.json
  assert_success

  assert_json '.classMap'
  assert_json '.events'
  assert_json '.version'
  assert_json_eq '.events | map(select(.http_server_request or .http_server_response)) | ((.[0].event == "call" and .[1].event == "return") and (.[1].parent_id == .[0].id))' "true"
}
  