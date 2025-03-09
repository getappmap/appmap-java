load '../helper'

init_plugin() {
    git clone --depth=1 https://github.com/applandinc/appmap-intellij-plugin.git
}

setup_file() {
  if [[ $JAVA_VERSION != 17.* ]]; then
    skip "needs Java 17"
  fi

  export AGENT_JAR="$(find_agent_jar)"

  cd test/intellij
  _configure_logging

  if [[ ! -d appmap-intellij-plugin ]]; then
    init_plugin
  fi
}

setup() {
  cd appmap-intellij-plugin
  rm -rf tmp/appmap

  ./gradlew clean
  mkdir -p build
  cp "${AGENT_JAR}" build/appmap-agent.jar
}

@test 'it works' {
  run ./gradlew :plugin-core:test --tests 'AppMapConfigFileTest.readConfigWithPath'
  assert_success

  output="$(< tmp/appmap/junit/appland_config_AppMapConfigFileTest_readConfigWithPath.appmap.json)"
  local lengths=( $(jq '(.events | length),(.classMap | length)' <<< "${output}") )
  # AppMap shouldn't be empty
  assert [ ${lengths[0]} -gt 0 ]
  assert [ ${lengths[1]} -gt 0 ]
}