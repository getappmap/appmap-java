load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'

init_example() (
    git clone --no-checkout https://github.com/playframework/play-samples.git
    cd play-samples
    git sparse-checkout set play-scala-rest-api-example
    local branch=3.0.x
    case "${JAVA_VERSION}" in
      1.8*)
        branch=2.8.x
        ;;
      11.*)
        branch=2.9.x
        ;;
    esac
    git checkout $branch
    cp ../logback-test.xml play-scala-rest-api-example/conf/.
)

setup_file() {
  export AGENT_JAR="$(find_agent_jar)"

  cd test/scala
  _configure_logging

  if [[ ! -d play-samples/play-scala-rest-api-example ]]; then
    init_example
  fi
}

setup() {
  cd play-samples/play-scala-rest-api-example
  rm -rf tmp/appmap
}

@test "it works" {
  # Run sbt with agent attached. This generated empty AppMaps with the previous version of the
  # agent.
  run \
    env JAVA_OPTS="-javaagent:$AGENT_JAR -Dappmap.recording.auto=true" sbt --batch test
  assert_success

  # sbt run with the agent generates two AppMaps. Examine the second (newer) one. Get the lengths of
  # the events and classMap arrays, make sure they're not empty
  local newest="tmp/appmap/java/$(ls -t tmp/appmap/java | head -1)"
  local lengths=( $(jq '(.events | length),(.classMap | length)' $newest) )
  assert [ ${lengths[0]} -gt 0 ]
  assert [ ${lengths[1]} -gt 0 ]

}
