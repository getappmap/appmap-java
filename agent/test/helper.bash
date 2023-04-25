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

# Start a PetClinic server. Note that the output from the printf's in this
# function don't get redirected, to make it easier to use from a shell. When you
# run it from a setup function, you should redirect fd 3.
start_petclinic() {
  mkdir -p test/petclinic/classes
  javac -d test/petclinic/classes test/petclinic/Props.java

  export LOG_DIR=$PWD/build/log
  mkdir -p ${LOG_DIR}

  export LOG=$PWD/build/fixtures/spring-petclinic/petclinic.log
  export WS_URL="http://localhost:8080"

  printf 'checking for running Petclinic server\n'
  
  if ! curl -Isf "${WS_URL}" >/dev/null 2>&1; then
    if [ $? -eq 7 ]; then
      printf '  server already running\n'
      exit 1
    fi
  fi

  printf '  starting PetClinic\n'
  WD=$PWD
  AGENT_JAR="$(find_agent_jar)"

  pushd build/fixtures/spring-petclinic >/dev/null
  ./mvnw -Dspring-boot.run.agents=$AGENT_JAR -Dspring-boot.run.jvmArguments="-Dappmap.config.file=$WD/test/petclinic/appmap.yml -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005" spring-boot:run &>$LOG  3>&- &
  popd >/dev/null

  export JVM_PID=$!
  while ! curl -Isf "${WS_URL}" >/dev/null; do
  if ! kill -0 "${JVM_PID}" 2> /dev/null; then
    printf '  failed!\n\nprocess exited unexpectedly:\n'
    cat $LOG 
    exit 1
  fi

  sleep 1
  done
  printf '  ok\n\n'
}

stop_petclinic() {
  kill ${JVM_PID}
}