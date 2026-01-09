#!/usr/bin/env bats

load '../helper'

sep="$JAVA_PATH_SEPARATOR"
AGENT_JAR="$(find_agent_jar)"

# Resolves to .../agent/test
TEST_DIR="$(dirname "$BATS_TEST_DIRNAME")"

# test_cp must include 'test' (for access package) and 'test/access' (for default package)
test_cp="${TEST_DIR}${sep}${TEST_DIR}/access"
java_cmd="java -javaagent:'${AGENT_JAR}' -cp '${test_cp}'"

setup() {
  cd "$BATS_TEST_DIRNAME" || exit
  javac -cp "${AGENT_JAR}" -sourcepath "$TEST_DIR" ./*.java
  _configure_logging
}

# The following test functions do some logging if BATS_VERBOSE_RUN is set (i.e.
# the --verbose-run switch was used when invoking bats).
#
# Strictly speaking, this isn't what --verbose-run is meant to mean. The
# functionaly it enables is pretty useless, though, so it seems harmless to
# hijack it.
@test "testProtected" {
  local cmd="${java_cmd} access.RecordPackage"
  [[ $BATS_VERBOSE_RUN == 1 ]] && echo "cmd: $cmd" >&3

  # Exactly 4 events means that MyClass.myPrivateMethod was not recorded
  eval "$cmd" | jq -e '.events | length | select(. == 4)'
  eval "$cmd" | jq -e '.events[1] | select(.event == "call" and .method_id == "myPackageMethod")'
}

@test "testPrivate" {
  local cmd="${java_cmd} -Dappmap.record.private=true access.RecordPackage"
  [[ $BATS_VERBOSE_RUN == 1 ]] && echo "cmd: $cmd" >&3

  # 6 events means that both myPackageMethod and myPrivateMethod were recorded
  eval "$cmd" | jq -e '.events | length | select(. == 6)'
  eval "$cmd" | jq -e '.events[3] | select(.event=="call" and .method_id=="myPrivateMethod")'
}

@test "outside git repo" {
  cp -v "appmap.yml" "$BATS_TEST_TMPDIR"/.
  cd "$BATS_TEST_TMPDIR"

  # sanity check
  run git rev-parse --show-top-level
  assert_output -p 'not a git repository'
  assert_failure

  local cmd="${java_cmd} access.RecordPackage"
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