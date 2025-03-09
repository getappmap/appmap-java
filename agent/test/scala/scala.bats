load '../helper'

setup_file() {
  export AGENT_JAR="$(find_agent_jar)"
  export FIXTURE_DIR="play-samples/play-scala-rest-api-example"
  cd test/scala
  _configure_logging

  if [[ ! -d "${FIXTURE_DIR}" ]]; then
    echo Run test_install first.
    exit 1
  fi
}

setup() {
  cd "${FIXTURE_DIR}"
  export APPMAP_OUTPUT_DIRECTORY="$(getcwd)/tmp/appmap"

  rm -rf "${APPMAP_OUTPUT_DIRECTORY}"
}

@test "it works" {
  # Run sbt with agent attached. This generated empty AppMaps with the previous version of the
  # agent.
  run \
    env JAVA_OPTS="-javaagent:$AGENT_JAR -Dappmap.recording.auto=true" sbt --batch test
  assert_success

  # sbt run with the agent generates two AppMaps. Examine the second (newer) one. Get the lengths of
  # the events and classMap arrays, make sure they're not empty
  local out="${APPMAP_OUTPUT_DIRECTORY}/java"
  local newest="${out}/$(ls -t "${out}" | head -1)"
  local lengths=( $(jq '(.events | length),(.classMap | length)' $newest) )
  assert [ ${lengths[0]} -gt 0 ]
  assert [ ${lengths[1]} -gt 0 ]

}
