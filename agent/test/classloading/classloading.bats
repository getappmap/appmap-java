#!/usr/bin/env bats

load '../helper'

setup_file() {
  export AGENT_JAR="$(find_agent_jar)"

  cd "$BATS_TEST_DIRNAME"
  _configure_logging
}

@test "ClassUtil.safeClassForName" {
  # It's nice to be able to cut and paste the run command. Written like this,
  # it's easy to do (the test for BATS_VERSION will add -q to the command line
  # when run as a test, but not when run in a shell).
  run \
    gradlew ${BATS_VERSION+-q} -PappmapJar="$AGENT_JAR" run --args "TestSafeClassForName"
  assert_success
}

@test "Proxy" {
  run --separate-stderr \
    gradlew ${BATS_VERSION+-q} -PappmapJar="$AGENT_JAR" run --args "TestProxy"
  assert_success
  assert_json_eq ".events[0].defined_class" "com.appland.appmap.test.fixture.helloworld.HelloWorld"
  assert_json_eq ".events[0].method_id" "getGreeting"
  assert_json_eq ".events[0].path" "lib/src/main/java/com/appland/appmap/test/fixture/helloworld/HelloWorld.java"
}

@test "Bootstrap Classpath" {
  # Regression test for fix that allows running agent on bootstrap classpath.
  # This verifies that:
  # 1. Agent.class.getResource() works when getClassLoader() returns null
  # 2. Git integration is automatically disabled on bootstrap classpath
  # 3. No NullPointerException occurs during agent initialization
  run \
    gradlew ${BATS_VERSION+-q} -PappmapJar="$AGENT_JAR" -PuseBootstrapClasspath=true run --args "TestBootstrapClasspath"
  assert_success

  # Verify the test confirmed running on bootstrap classpath with Git disabled
  assert_output --partial "SUCCESS: Agent running on bootstrap classpath with Git disabled"
}
