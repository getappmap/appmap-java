#!/usr/bin/env bats

load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'

sep="$JAVA_PATH_SEPARATOR"
AGENT_JAR="$(find_agent_jar)"
wd=$(getcwd)
test_cp="${wd}/test/access${sep}${wd}/build/classes/java/test"
java_cmd="java -javaagent:'${AGENT_JAR}' -cp '${test_cp}'"

setup() {
  javac -cp "${AGENT_JAR}${sep}${test_cp}" test/access/*.java

  cd test/access
  _configure_logging
}

# The following test functions do some logging if BATS_VERBOSE_RUN is set (i.e.
# the --verbose-run switch was used when invoking bats).
#
# Strictly speaking, this isn't what --verbose-run is meant to mean. The
# functionaly it enables is pretty useless, though, so it seems harmless to
# hijack it.
@test "testProtected" {
  local cmd="${java_cmd} RecordPackage"
  [[ $BATS_VERBOSE_RUN == 1 ]] && echo "cmd: $cmd" >&3

  # Exactly 4 events means that MyClass.myPrivateMethod was not recorded
  eval "$cmd" | jq -e '.events | length | select(. == 4)'
  eval "$cmd" | jq -e '.events[1] | select(.event == "call" and .method_id == "myPackageMethod")'
}

@test "testPrivate" {
  local cmd="${java_cmd} -Dappmap.record.private=true RecordPackage"
  [[ $BATS_VERBOSE_RUN == 1 ]] && echo "cmd: $cmd" >&3

  # 6 events means that both myPackageMethod and myPrivateMethod were recorded
  eval "$cmd" | jq -e '.events | length | select(. == 6)'
  eval "$cmd" | jq -e '.events[3] | select(.event=="call" and .method_id=="myPrivateMethod")'
}

@test "outside git repo" {
  cp -v "$(_top_level)/agent/appmap.yml" "$BATS_TEST_TMPDIR"/.
  cd "$BATS_TEST_TMPDIR"

  # sanity check
  run git rev-parse --show-top-level
  assert_output -p 'not a git repository'
  assert_failure

  local cmd="${java_cmd} RecordPackage"
  [[ $BATS_VERBOSE_RUN == 1 ]] && echo "cmd: $cmd" >&3
  run bash -c "eval \"$cmd\" 2>/dev/null"
  assert_success
  local recording="${output}"
  run jq -e '.events | length | select(. == 4)' <<< "${recording}"
  assert_success

  # metadata shouldn't have any git info
  run jq '.metadata.git' <<< "${recording}"
  assert_output 'null'
}

@test "unnamed package" {
  local cmd="${java_cmd} RecordUnnamed"
  [[ $BATS_VERBOSE_RUN == 1 ]] && echo "cmd: $cmd" >&3
  eval "$cmd" | jq -e '.events | length | select(. == 2)'
  eval "$cmd" | jq -e '.events[0] | select(.event == "call" and .method_id == "getGreetingWithPunctuation")'
}