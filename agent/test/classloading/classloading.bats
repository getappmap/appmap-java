load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'

setup_file() {
  export AGENT_JAR="$(find_agent_jar)"

  cd test/classloading
}

@test "ClassUtil.safeClassForName" {
  # It's nice to be able to cut and paste the run command. Written like this,
  # it's easy to do (the test for BATS_VERSION will add -q to the command line
  # when run as a test, but not when run in a shell).
  run \
    ./gradlew ${BATS_VERSION+-q} -PappmapJar="$AGENT_JAR" run --args "TestSafeClassForName"
  assert_success
}
