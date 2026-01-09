#!/usr/bin/env bats

load '../helper'

AGENT_JAR="$(find_agent_jar)"

# Resolves to .../agent/test
TEST_DIR="$(dirname "$BATS_TEST_DIRNAME")"
java_cmd="java -javaagent:'${AGENT_JAR}' -cp '${TEST_DIR}'"

setup() {
  cd "$BATS_TEST_DIRNAME" || exit
  javac -cp "${AGENT_JAR}" -sourcepath "$TEST_DIR" ./*.java
  _configure_logging
}

@test "disabled value" {
  local cmd="${java_cmd} -Dappmap.event.disableValue=true event.DisabledValue"
  [[ $BATS_VERBOSE_RUN == 1 ]] && echo "cmd: $cmd" >&3
  
  local output
  output=$(eval "$cmd")

  echo "$output" | jq -e '.events[0] | select(.event=="call" 
    and .parameters[0].value == "< disabled >" 
    and .receiver.value == "< disabled >")'
  echo "$output" | jq -e '.events[1] | select(.event=="return" 
    and .return_value.value == "< disabled >")'
}