_shared_setup() {
  local fixtureSrc="$TOP_LEVEL/agent/src/test/fixture"
  tar -C "${fixtureSrc}/shared" -c -f - . | tar -C "${FIXTURE_DIR}" -x -f -
  local testdir="$(basename ${BATS_TEST_DIRNAME})"
  if [[ -d "${fixtureSrc}/${testdir}" ]]; then
    tar -C "${fixtureSrc}/${testdir}" -c -f - . | tar -C "${FIXTURE_DIR}" -x -f -
  fi

  _configure_logging
}
