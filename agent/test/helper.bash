#!/usr/bin/env bats
#
# Helper methods for tests

_curl() {
  curl -H 'Accept: application/json' "${@}"
}

_appmap() {
  java -jar /appmap.jar -d /spring-petclinic "${@}"
}

start_recording() {
  _curl -sXPOST "${WS_URL}/_appmap/record"
}

stop_recording() {
  output="$(_curl -sXDELETE ${WS_URL}/_appmap/record | tee /dev/stderr)"
}

teardown() {
  stop_recording
}

print_debug() {
  local query="${1}"
  local result="${2}"

  if [[ ! -z "${DEBUG_JSON}" ]]; then
    echo >&3
    echo "query: ${query}" >&3
    echo >&3
    echo "output: '${output}'" >&3
    echo >&3
    echo "result: '${result}'" >&3
    echo >&3
  fi
}

assert_json() {
  : "${output?}"

  # Expect a jq query as $1. Pipe it into `select`, so null values
  # will show up as a empty strings, rather than "null". (See
  # discussion here: https://github.com/stedolan/jq/issues/24)
  local query="${1?} | select(. == null | not)"
  local result=$(jq -r "${query}" <<< "${output}")

  print_debug "${query}" "${result}"
  refute [ -z "${result}" ]
}

assert_json_eq() {
  : "${output?}"

  local query="${1?} | select(. == null | not)"
  local expected_value="${2}"
  local result=$(jq -r "${query}" <<< "${output}")

  print_debug "${query}" "${result}"

  assert [ "${result}" == "${expected_value}" ]
}

assert_json_contains() {
  : "${output?}"

  local query="${1?} | select(. == null | not)"
  local match="${2}"
  local result=$(jq -r "${query}" <<< "${output}")

  print_debug "${query}" "${result}"
  assert grep -q "${match}" <<< "${result}"
}

assert_json_not_contains() {
  : "${output?}"

  local query="${1?}"

  refute jq -er "${query}" <<< "${output}"
}

find_agent_jar() {
  echo "$PWD/build/libs/$(ls build/libs | grep 'appmap-[[:digit:]]')"
}