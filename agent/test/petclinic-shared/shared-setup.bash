_shared_setup() {
  local fixtureSrc="$(git rev-parse --show-toplevel)/agent/src/test/fixture"
  tar -C "${fixtureSrc}/shared" -c -f - . | tar -C "${FIXTURE_DIR}" -x -f - 
  local testdir="$(basename ${BATS_TEST_DIRNAME})"
  if [[ -d "${fixtureSrc}/${testdir}" ]]; then
    tar -C "${fixtureSrc}/${testdir}" -c -f - . | tar -C "${FIXTURE_DIR}" -x -f - 
  fi

  cp "$(dirname ${BASH_SOURCE[0]})"/appmap-log.local.properties "${FIXTURE_DIR}"
}
