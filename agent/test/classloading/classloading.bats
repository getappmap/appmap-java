load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'

setup_file() {
  export AGENT_JAR="$(find_agent_jar)"

  cd test/classloading
}

@test "Can load a class from a runtime jar" {
  run ./gradlew -q -PappmapJar="$AGENT_JAR" run
  assert_success
}
