#!/usr/bin/env bats
#
# Helper methods for tests

export JAVA_PATH_SEPARATOR="$(java -XshowSettings:properties 2>&1 | awk '/path.separator/ {printf("%s", $3)}')"

_curl() {
  curl -sfH 'Accept: application/json,*/*' "${@}"
}

_appmap() {
  java -jar /appmap.jar -d /spring-petclinic "${@}"
}

start_recording() {
  _curl -sXPOST "${WS_URL}/_appmap/record"
}

stop_recording() {
  local out="${1:-$BATS_TEST_TMPDIR/stop_recording_output}"
  _curl -sXDELETE -o "$out" "${WS_URL}/_appmap/record" || true
  [ -f $out ] && output="$(< $out)" || true
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
    echo -e "output:\n'${output}'" >&3
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

  assert_not_equal "${result}" ""
}

assert_json_eq() {
  : "${output?}"

  local query="${1?} | select(. == null | not)"
  local expected_value="${2}"
  local result=$(jq -r "${query}" <<< "${output}")

  print_debug "${query}" "${result}"

  assert_equal "${result}" "${expected_value}"
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
  echo "$(git rev-parse --show-toplevel)/agent/build/libs/$(ls build/libs | grep 'appmap-[[:digit:]]')"
}

check_ws_running() {
  printf 'checking for running web server\n'

  if ! curl -Isf "${WS_URL}" >/dev/null 2>&1; then
    if [ $? -eq 7 ]; then
      printf '  server already running\n'
      exit 1
    fi
  fi
}

wait_for_ws() {
  while ! curl -Isf "${WS_URL}" >/dev/null; do
    if ! jcmd $JVM_MAIN_CLASS VM.uptime >/dev/null; then
      echo "$JVM_MAIN_CLASS failed"
      exit 1
    fi
    sleep 1
  done
  printf '  ok\n\n'
}

wait_for_mvn() {
  local mvn_pid=$1

  # The only thing special about the VM.uptime command is that it's fast, and
  # the output is small.
  local uptime="jcmd ${JVM_MAIN_CLASS} VM.uptime"
  while ! ${uptime} 2>/dev/null; do
    if ! ps -p $mvn_pid >/dev/null; then
      echo "mvn failed"
      cat $LOG
      exit 1
    fi
    sleep 1
  done
  # Final check, this will fail if the server didn't start
  echo "final check: $(${uptime})" >&3
}

# Start a PetClinic server. Note that the output from the printf's in this
# function don't get redirected, to make it easier to use from a shell. When you
# run it from a setup function, you should redirect fd 3.
start_petclinic() {
  local jvmargs="$@"

  mkdir -p test/petclinic/classes
  javac -d test/petclinic/classes test/petclinic/Props.java

  export LOG_DIR=$PWD/build/log
  mkdir -p ${LOG_DIR}

  export LOG=$PWD/build/fixtures/spring-petclinic/petclinic.log
  export WS_SCHEME="http" WS_HOST="localhost" WS_PORT="8080"
  export WS_URL="${WS_SCHEME}://${WS_HOST}:${WS_PORT}"

  check_ws_running

  printf '  starting PetClinic\n'
  WD=$PWD
  AGENT_JAR="$(find_agent_jar)"

  pushd build/fixtures/spring-petclinic >/dev/null
  ./mvnw --quiet -DskipTests -Dcheckstyle.skip=true -Dspring-javaformat.skip=true \
    -Dspring-boot.run.agents=$AGENT_JAR -Dspring-boot.run.jvmArguments="-Dappmap.config.file=$WD/test/petclinic/appmap.yml $jvmargs" \
    spring-boot:run &>$LOG  3>&- &
  export JVM_MAIN_CLASS=PetClinicApplication
  wait_for_mvn $!

  popd >/dev/null


  wait_for_ws
}

start_petclinic_fw() {
  WD="$(git rev-parse --show-toplevel)/agent"
  export LOG_DIR="$WD/build/log"
  mkdir -p ${LOG_DIR}

  export LOG="$WD/build/fixtures/spring-framework-petclinic/petclinic.log"
  export WS_SCHEME="http" WS_HOST="localhost" WS_PORT="8080"
  export WS_URL="${WS_SCHEME}://${WS_HOST}:${WS_PORT}"

  check_ws_running

  printf '  starting PetClinic (framework)\n'
  AGENT_JAR="$(find_agent_jar)"

  pushd build/fixtures/spring-framework-petclinic >/dev/null
  ./mvnw --quiet -DskipTests -Dcheckstyle.skip=true -Dspring-javaformat.skip=true \
    -Djetty.deployMode=FORK -Djetty.jvmArgs="-javaagent:$AGENT_JAR -Dappmap.config.file=$WD/test/petclinic/appmap.yml" \
    jetty:run-war &>$LOG  3>&- &
  export JVM_MAIN_CLASS=JettyForkedChild
  wait_for_mvn $!
  popd >/dev/null

  wait_for_ws
}

stop_ws() {
  # curl doesn't like it when the server exits. Assume the request was
  # successful, then wait for the main class to finish.
  curl -XDELETE "${WS_URL}"/exit >&3 || true

  for i in {1..30}; do
    if ! jcmd $JVM_MAIN_CLASS VM.uptime >&3; then
      break;
    fi
    sleep 1
  done

  if jvm $JVM_MAIN_CLASS VM.update >&3; then
    echo "$JVM_MAIN_CLASS didn't exit"
    if [[ ! -z "$LOG" ]]; then
      cat "$LOG" >&3
    fi
    exit 1;
  fi

}

wait_for_glob() {
  local glob="$1"
  for i in {1..10}; do
    if compgen -G "$glob" >/dev/null; then
      break;
    fi
    sleep .5
  done
}
