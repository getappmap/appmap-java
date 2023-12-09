load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'

setup_file() {
  export AGENT_JAR="$(find_agent_jar)"

  cd test/classloading
  _configure_logging
}

@test "ClassUtil.safeClassForName" {
  # It's nice to be able to cut and paste the run command. Written like this,
  # it's easy to do (the test for BATS_VERSION will add -q to the command line
  # when run as a test, but not when run in a shell).
  run \
    ./gradlew ${BATS_VERSION+-q} -PappmapJar="$AGENT_JAR" run --args "TestSafeClassForName"
  assert_success
}

@test "Proxy" {
  run \
    ./gradlew ${BATS_VERSION+-q} -PappmapJar="$AGENT_JAR" run --args "TestProxy"
  assert_success
  assert_json_eq ".events[0].defined_class" "com.appland.appmap.classloading.helloworld.HelloWorld"
  assert_json_eq ".events[0].method_id" "getGreeting"
  assert_json_eq ".events[0].path" "lib/src/main/java/com/appland/appmap/classloading/helloworld/HelloWorld.java"
}
