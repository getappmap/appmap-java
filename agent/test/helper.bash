#!/usr/bin/env bats
#
# Helper methods for tests

export JAVA_PATH_SEPARATOR="$(java -XshowSettings:properties 2>&1 | awk '/path.separator/ {printf("%s", $3)}')"
[ -z "$JAVA_HOME" ] && export JAVA_HOME="$(java -XshowSettings:properties 2>&1 | awk '/java.home/ {printf("%s", $3)}')"
export JAVA_HOME
source "$JAVA_HOME/release"
export JAVA_VERSION

# Because the agent appends to the boot classpath, the JVM disables the class-data sharing
# optimization (described here: https://nipafx.dev/java-application-class-data-sharing/) and
# issues a warning. That warning confuses our tests, so set -Xshare:off to preempt it.
export JAVA_OUTPUT_OPTIONS="-Xshare:off"


_curl() {
  curl -sfH 'Accept: application/json,*/*' "${@}"
}

_appmap() {
  java -jar /appmap.jar -d /spring-petclinic "${@}"
}

start_recording() {
  _curl -sXPOST "${WS_URL}/_appmap/record"
}

_tests_helper() {
  export BATS_LIB_PATH=${BATS_LIB_PATH:-"/usr/lib"}
  if type -t bats_load_library &>/dev/null; then
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_require_minimum_version 1.5.0
  fi
}

_tests_helper

export LC_ALL=C

stop_recording() {
  local out="${1:-$BATS_TEST_TMPDIR/stop_recording_output}"
  _curl -sXDELETE -o "$out" "${WS_URL}/_appmap/record" || true
  [ -f $out ] && output="$(< $out)" || true
}

teardown() {
  stop_recording
  if [[ -z "$BATS_TEST_COMPLETED" ]]; then
    # Letting output go to stdout is more helpful than redirecting to fd 3.
    echo "${BATS_TEST_NAME} failed"
    [[ -n "$FIXTURE_DIR" ]] && cat "${FIXTURE_DIR}/appmap.log"
  fi
}

print_debug() {
  local query="${1}"
  local result="${2}"

  if [[ $BATS_VERBOSE_RUN == 1 ]]; then
    echo >&3
    echo -e "output:\n'${output}'" >&3
    echo >&3
    echo "query: ${query}" >&3
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
  local msg="${3:-json query: $1}"
  local result=$(jq -r "${query}" <<< "${output}")

  print_debug "${query}" "${result}"

  [[ ! -z "$msg" ]] && assert_equal "${result}" "${expected_value}" "${msg}" || assert_equal "${result}" "${expected_value}"
}

assert_json_contains() {
  : "${output?}"

  local query="${1?} | select(. == null | not)"
  local match="${2}"
  local result=$(jq -r "${query}" <<< "${output}")

  print_debug "${query}" "${result}"

  grep -q "${match}" <<< "${result}"
  assert_equal $? 0 "no match for '${match}' in '${result}'"
}

assert_json_not_contains() {
  : "${output?}"

  local query="${1?}"

  refute jq -er "${query}" <<< "${output}"
}

_top_level() {
  git rev-parse --show-toplevel
}


find_agent_jar() {
  if [[ -n "$AGENT_JAR" ]]; then
    echo "$AGENT_JAR"
    return
  fi
  find "$(_top_level)" -name 'appmap-[[:digit:]]*.jar'
}

export AGENT_JAR="$(find_agent_jar)"

find_annotation_jar() {
  if [[ -n "$ANNOTATION_JAR" ]]; then
    echo "$ANNOTATION_JAR"
    return
  fi
  find "$(_top_level)" -name 'annotation-[[:digit:]]*.jar'
}

export ANNOTATION_JAR="$(find_annotation_jar)"

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
  local url="${1:-$WS_URL}"
  while ! curl -Isf "${url}" >/dev/null; do
    if ! jcmd $JVM_MAIN_CLASS VM.uptime >&/dev/null; then
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
  while ! ${uptime} >&/dev/null; do
    if ! ps -p $mvn_pid >&/dev/null; then
      echo "mvn failed"
      cat $LOG
      exit 1
    fi
    sleep 1
  done
  # Final check, this will fail if the server didn't start
  ${uptime} >&/dev/null
}

# Start a PetClinic server. Note that the output from the printf's in this
# function don't get redirected, to make it easier to use from a shell. When you
# run it from a setup function, you should redirect fd 3.
start_petclinic() {
  local jvmargs="$@"

  mkdir -p test/petclinic/classes
  javac -d test/petclinic/classes test/petclinic/Props.java

  WD=$(getcwd)

  export LOG_DIR=$WD/build/log
  mkdir -p ${LOG_DIR}

  local fixture_dir="$WD/build/fixtures/spring-petclinic"
  export LOG="${fixture_dir}/petclinic.log"
  export WS_SCHEME="http" WS_HOST="localhost" WS_PORT="8080"
  export WS_URL="${WS_SCHEME}://${WS_HOST}:${WS_PORT}"

  check_ws_running

  printf '  starting PetClinic\n'
  AGENT_JAR="$(find_agent_jar)"

  local cfg="$WD/test/petclinic/appmap.yml"
  local out="${fixture_dir}/tmp/appmap"
  pushd build/fixtures/spring-petclinic >/dev/null
  ./mvnw ${MAVEN_PROFILE} ${BATS_VERSION+--quiet} -DskipTests -Dcheckstyle.skip=true -Dspring-javaformat.skip=true \
    -Dspring-boot.run.agents=$AGENT_JAR \
    -Dspring-boot.run.jvmArguments="-Dappmap.config.file='${cfg}' -Dappmap.output.directory='${out}' $jvmargs" \
    spring-boot:run \
    &>$LOG  3>&- &
  export JVM_MAIN_CLASS=PetClinicApplication
  wait_for_mvn $!

  popd >/dev/null


  wait_for_ws
}

start_petclinic_fw() {
  WD=$(getcwd)
  export LOG_DIR="$WD/build/log"
  mkdir -p ${LOG_DIR}

  local fixture_dir="$WD/build/fixtures/spring-framework-petclinic"
  export LOG="${fixture_dir}/petclinic.log"

  export LOG="${fixture_dir}/petclinic.log"
  export WS_SCHEME="http" WS_HOST="localhost" WS_PORT="8080"
  export WS_URL="${WS_SCHEME}://${WS_HOST}:${WS_PORT}"

  check_ws_running

  printf '  starting PetClinic (framework)\n'
  AGENT_JAR="$(find_agent_jar)"

  local cfg="$WD/test/petclinic/appmap.yml"
  local out="${fixture_dir}/tmp/appmap"
  pushd build/fixtures/spring-framework-petclinic >/dev/null
  ./mvnw --quiet -DskipTests -Dcheckstyle.skip=true -Dspring-javaformat.skip=true \
    -Djetty.deployMode=FORK -Djetty.jvmArgs="-javaagent:$AGENT_JAR -Dappmap.config.file=${cfg} -Dappmap.output.directory=${out}" \
    jetty:run-war &>$LOG  3>&- &
  export JVM_MAIN_CLASS=JettyForkedChild
  wait_for_mvn $!
  popd >/dev/null

  wait_for_ws
}

stop_ws() {
  # curl doesn't like it when the server exits. Assume the request was
  # successful, then wait for the main class to finish.
  curl -sXDELETE "${WS_URL}"/exit  || true

  for i in {1..30}; do
    if ! jcmd $JVM_MAIN_CLASS VM.uptime >&/dev/null ; then
      break;
    fi
    sleep 1
  done

  if jcmd $JVM_MAIN_CLASS VM.update >&/dev/null; then
    echo "$JVM_MAIN_CLASS didn't exit"
    if [[ ! -z "$LOG" ]]; then
      cat "$LOG" >&3
    fi
    exit 1;
  fi

}

wait_for_glob() {
  local glob="$1"
  echo "waiting for glob ${glob}" >&3
  for i in {1..10}; do
    if compgen -G "$glob" >/dev/null; then
      break;
    fi
    sleep .5
  done
}

_configure_logging() {
  local logConfigDir="$(_top_level)/agent/test/shared"
  export APPMAP_DISABLELOGFILE=true
  export JUL_CONFIG="${logConfigDir}/java-logging.properties"
}

getcwd() {
  # This seems slightly ridiculous. But, it produces a path that's understood by both Bash and Java,
  # and it works in all dev and CI environments.
  git rev-parse --show-toplevel --show-prefix | tr -s '\n' '/'
}
