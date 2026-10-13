#!/usr/bin/env bats
#
# Tests for the git metadata that gets written into a recording.

load '../helper'

sep="$JAVA_PATH_SEPARATOR"
AGENT_JAR="$(find_agent_jar)"

# Resolves to .../agent/test
TEST_DIR="$(dirname "$BATS_TEST_DIRNAME")"

# As in the access tests, the classpath must include 'test' (for the access
# package) and 'test/access' (for the default package).
test_cp="${TEST_DIR}${sep}${TEST_DIR}/access"
java_cmd="java -javaagent:'${AGENT_JAR}' -cp '${test_cp}'"

REMOTE_URL="https://example.com/appmap-java-test.git"

setup() {
  cd "${TEST_DIR}/access" || exit
  javac -cp "${AGENT_JAR}" -sourcepath "$TEST_DIR" ./*.java
  _configure_logging

  # Don't let the developer's global git config (e.g. a missing user.name, or
  # init.defaultBranch) interfere.
  export GIT_CONFIG_GLOBAL="${BATS_TEST_TMPDIR}/gitconfig"
  export GIT_CONFIG_SYSTEM=/dev/null
  export GIT_AUTHOR_NAME="Test" GIT_AUTHOR_EMAIL="test@example.com"
  export GIT_COMMITTER_NAME="Test" GIT_COMMITTER_EMAIL="test@example.com"
  : > "$GIT_CONFIG_GLOBAL"
}

# Create a repo with a remote and a single commit in $1. The sources of the
# fixture get committed too, so the agent has a source root to resolve them
# against.
init_repo() {
  local repo="$1"

  git init -q -b main "$repo"
  git -C "$repo" remote add origin "$REMOTE_URL"
  cp "$BATS_TEST_DIRNAME/appmap.yml" "$repo"
  mkdir -p "$repo/src/main/java/access"
  cp "${TEST_DIR}"/access/*.java "$repo/src/main/java/access"
  git -C "$repo" add appmap.yml src
  git -C "$repo" commit -qm 'initial commit'
}

# Run a recording with the working directory set to $1, leaving the recording in
# $output and the agent's diagnostics in $stderr.
record_in() {
  local dir="$1"

  cd "$dir" || exit
  [[ $BATS_VERBOSE_RUN == 1 ]] && echo "cmd: ${java_cmd} access.RecordPackage" >&3
  run --separate-stderr bash -c "eval \"${java_cmd} access.RecordPackage\""
  assert_success
}

@test "git metadata in a repository" {
  local repo="${BATS_TEST_TMPDIR}/repo"
  init_repo "$repo"

  record_in "$repo"

  assert_json_eq '.metadata.git.repository' "$REMOTE_URL"
  assert_json_eq '.metadata.git.branch' 'main'
  assert_json_eq '.metadata.git.commit' "$(git -C "$repo" rev-parse HEAD)"
  assert_json_eq '.events[0].path' 'src/main/java/access/MyClass.java'
}

@test "git metadata in a linked worktree" {
  local repo="${BATS_TEST_TMPDIR}/repo"
  local worktree="${BATS_TEST_TMPDIR}/worktree"
  init_repo "$repo"
  git -C "$repo" worktree add -q -b feature "$worktree"

  record_in "$worktree"

  refute grep -q 'unsupported configuration' <<< "$stderr"
  assert_json_eq '.metadata.git.repository' "$REMOTE_URL"
  assert_json_eq '.metadata.git.branch' 'feature'
  assert_json_eq '.metadata.git.commit' "$(git -C "$worktree" rev-parse HEAD)"
  assert_json_eq '.events[0].path' 'src/main/java/access/MyClass.java'
}

@test "git metadata in a detached linked worktree" {
  local repo="${BATS_TEST_TMPDIR}/repo"
  local worktree="${BATS_TEST_TMPDIR}/worktree"
  init_repo "$repo"
  local commit="$(git -C "$repo" rev-parse HEAD)"
  git -C "$repo" worktree add -q --detach "$worktree"

  record_in "$worktree"

  refute grep -q 'unsupported configuration' <<< "$stderr"
  assert_json_eq '.metadata.git.repository' "$REMOTE_URL"
  assert_json_eq '.metadata.git.branch' "$commit"
  assert_json_eq '.metadata.git.commit' "$commit"
  assert_json_eq '.events[0].path' 'src/main/java/access/MyClass.java'
}
