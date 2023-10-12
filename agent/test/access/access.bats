#!/usr/bin/env bats

load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'

sep="$JAVA_PATH_SEPARATOR"
appmap_jar="$(find_agent_jar)"
test_cp="test/access${sep}build/classes/java/test"
java_cmd="java -javaagent:'${appmap_jar}' -cp '${test_cp}'"

setup() {
  echo "javac -cp "${appmap_jar}${sep}${test_cp}" test/access/*.java" >&3
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

