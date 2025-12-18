#!/usr/bin/env bats

load ${BATS_TEST_DIRNAME}/../helper.bash

@test "logging output verification" {
  local agent_jar
  agent_jar=$(find_agent_jar)

  # Create a temporary directory for compiled classes
  local build_dir="$BATS_TEST_TMPDIR/classes"
  mkdir -p "$build_dir"

  # Compile the source file
  # We assume javac is available in the path or via JAVA_HOME which helper.bash might set up
  javac -d "$build_dir" "${BATS_TEST_DIRNAME}/com/example/HelloWorld.java"

  cd ${BATS_TEST_DIRNAME}

  export APPMAP_DEBUG=true
  export APPMAP_CONFIG=$(pwd)/appmap.yml
  export CLASSPATH="$build_dir:${agent_jar}"

  run java \
    -javaagent:"${agent_jar}" \
    com.example.HelloWorld

  assert_success

  # Verify 'className' is logged at TRACE level (i.e., not present at DEBUG)
  refute_output --partial "DEBUG ClassFileTransformer.transform: className: com.example.HelloWorld"
  
  # Verify 'hooks applied to' is logged at DEBUG level
  assert_output --partial "DEBUG ClassFileTransformer.transform: hooks applied to com.example.HelloWorld"

  # Verify 'hooked' messages are logged at DEBUG level
  assert_output --partial "DEBUG ClassFileTransformer.applyHooks: hooked com.example.HelloWorld.sayHello()V on"

  # Verify improved config logging format (YAML-like instead of JSON)
  assert_output --partial "config: name: logging-test-app"
  assert_output --partial "packages: "
  assert_output --partial "  - path: com.example"
  
  # Verify System properties are now at DEBUG level (not INFO)
  # "INFO Agent.premain: System properties:" should NOT be present
  refute_output --partial "INFO Agent.premain: System properties:"
  # But if we are in debug mode (which we are), it should be visible at DEBUG level
  assert_output --partial "DEBUG Agent.premain: System properties:"
}
