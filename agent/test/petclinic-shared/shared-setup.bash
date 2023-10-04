_shared_setup() {
  tar -C "./src/test/fixture" -c -f - .| tar -C "${FIXTURE_DIR}" -x -f - >&3
}
