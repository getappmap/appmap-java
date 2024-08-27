#!/usr/bin/env bats

load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'

sep="$JAVA_PATH_SEPARATOR"
AGENT_JAR="$(find_agent_jar)"
wd=$(getcwd)
test_cp="${wd}/test/event${sep}${wd}/build/classes/java/test"
java_cmd="java -javaagent:'${AGENT_JAR}' -cp '${test_cp}'"

setup() {
  javac -cp "${AGENT_JAR}${sep}${test_cp}" test/event/*.java

  cd test/event
  _configure_logging
}

@test "disabled value" {
  local cmd="${java_cmd} -Dappmap.event.disableValue=true DisabledValue"
  [[ $BATS_VERBOSE_RUN == 1 ]] && echo "cmd: $cmd" >&3
  
  local output
  output=$(eval "$cmd")

  echo "$output" | jq -e '.events[0] | select(.event=="call" 
    and .parameters[0].value == "< disabled >" 
    and .receiver.value == "< disabled >")'
  echo "$output" | jq -e '.events[1] | select(.event=="return" 
    and .return_value.value == "< disabled >")'
}