#!/usr/bin/env bats

load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'

sep="$JAVA_PATH_SEPARATOR"
appmap_jar="$(find_agent_jar)"
wd="$(git rev-parse --show-toplevel)"/agent
test_cp="${wd}/test/access${sep}${wd}/build/classes/java/test"
java_cmd="java -javaagent:'${appmap_jar}' -cp '${test_cp}'"

setup() {
  javac -cp "${appmap_jar}${sep}${test_cp}" test/access/*.java
}

@test "testProtected" {
  local cmd="${java_cmd} RecordPackage"

  # Exactly 4 events means that MyClass.myPrivateMethod was not recorded
  eval "$cmd" | jq -e '.events | length | select(. == 4)'
  eval "$cmd" | jq -e '.events[1] | select(.event == "call" and .method_id == "myPackageMethod")'
}

@test "testPrivate" {
  local cmd="${java_cmd} -Dappmap.record.private=true RecordPackage"

  # 6 events means that both myPackageMethod and myPrivateMethod were recorded
  eval "$cmd" | jq -e '.events | length | select(. == 6)'
  eval "$cmd" | jq -e '.events[3] | select(.event=="call" and .method_id=="myPrivateMethod")'
}

@test "outside git repo" {
  cp appmap.yml "$BATS_TEST_TMPDIR"/.
  cd "$BATS_TEST_TMPDIR"

  # sanity check
  run git rev-parse --show-top-level
  assert_output -p 'not a git repository'
  assert_failure

  local cmd="${java_cmd} RecordPackage"
  run bash -c "eval \"$cmd\" 2>/dev/null"
  assert_success
  local recording="${output}"
  run jq -e '.events | length | select(. == 4)' <<< "${recording}"
  assert_success

  # metadata shouldn't have any git info
  run jq '.metadata.git' <<< "${recording}"
  assert_output 'null'
}