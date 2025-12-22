#!/usr/bin/env bats
# shellcheck disable=SC2164

load '../helper'

sep="$JAVA_PATH_SEPARATOR"
AGENT_JAR="$(find_agent_jar)"
java_cmd="java -cp ${BATS_TEST_DIRNAME}/build -javaagent:'${AGENT_JAR}'"

setup() {
  cd "${BATS_TEST_DIRNAME}"

  mkdir -p build
  # Compile tests. Output to build so package structure 'test/pkg' works.
  javac -d ./build UnicodeTest.java

  # Require the agent jar on the classpath to find the Recording class.
  javac -cp "${AGENT_JAR}" -d ./build ReadFullyTest.java

  rm -rf "${BATS_TEST_DIRNAME}/tmp/appmap"
  _configure_logging
}

@test "AppMap file encoding with Windows-1252" {
  # Run with windows-1252 encoding.
  # We assert that the generated file is valid UTF-8 and contains the correct characters,
  # even though the JVM default encoding is Windows-1252.
  local cmd="${java_cmd} -Dfile.encoding=windows-1252 -Dappmap.recording.auto=true test.pkg.UnicodeTest"
  [[ $BATS_VERBOSE_RUN == 1 ]] && echo "cmd: $cmd" >&3

  eval "$cmd"

  # Verify the output file exists — it should be the only AppMap file generated, with random name
  # so glob for tmp/appmap/java/*.appmap.json
  appmap_file=$(ls tmp/appmap/java/*.appmap.json)
  [ -f "$appmap_file" ]

  # Verify it is valid JSON
  jq . "$appmap_file" > /dev/null

  # Verify it is valid UTF-8
  iconv -f UTF-8 -t UTF-8 "$appmap_file" > /dev/null

  # Verify it contains the expected Unicode characters
  grep -q "Euro: €, Accent: é, Quote: „" "$appmap_file"
}

@test "Recording.readFully works with Windows-1252 default encoding" {
  # Run ReadFullyTest with windows-1252 default encoding.
  # We also need to add the agent jar to the classpath so it can find the Recording class.
  local cmd="java -cp ${BATS_TEST_DIRNAME}/build${sep}${AGENT_JAR} -Dfile.encoding=windows-1252 test.pkg.ReadFullyTest"
  [[ $BATS_VERBOSE_RUN == 1 ]] && echo "cmd: $cmd" >&3

  run eval "$cmd"

  [ "$status" -eq 0 ]
  [[ "$output" == *"Check: ⚠️ Привет"* ]]
}

teardown() {
  rm -rf tmp
  rm -rf build
}
