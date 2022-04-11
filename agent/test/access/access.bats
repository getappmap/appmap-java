#!/usr/bin/env bats

load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'

appmap_jar="$(ls build/libs/appmap-[0-9]*.jar)"
test_cp=test/access:build/classes/java/test

setup() {
  javac -cp "${appmap_jar}:${test_cp}" test/access/*.java
}

@test "testProtected" {
  local cmd="java -javaagent:'${appmap_jar}' -cp '${test_cp}' RecordPackage"

  # Exactly 4 events means that MyClass.myPrivateMethod was not recorded
  eval "$cmd" | jq -e '.events | length | select(. == 4)'
  eval "$cmd" | jq -e '.events[1] | select(.event == "call" and .method_id == "myPackageMethod")'
}

@test "testPrivate" {
  local cmd="java -javaagent:${appmap_jar} -cp ${test_cp} -Dappmap.record.private=true RecordPackage"

  # 6 events means that both myPackageMethod and myPrivateMethod were recorded
  eval "$cmd" | jq -e '.events | length | select(. == 6)'
  eval "$cmd" | jq -e '.events[3] | select(.event=="call" and .method_id=="myPrivateMethod")'
}

