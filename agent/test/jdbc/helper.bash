#!/usr/bin/env bash

# generate_sql_snapshots <appmap_dir> <target_dir> <file_glob>
#
# Generates .sql files in <target_dir> from .appmap.json files in <appmap_dir>
# that match <file_glob>.
generate_sql_snapshots() {
  local appmap_dir="$1"
  local target_dir="$2"
  local file_glob="$3"

  mkdir -p "$target_dir"

  for f in "$appmap_dir"/$file_glob; do
    if [ -f "$f" ]; then
      local snapshot_name
      snapshot_name=$(basename "$f" .appmap.json).sql
      jq -r '.events[] | select(.sql_query) | .sql_query.sql' "$f" >"$target_dir/$snapshot_name"
    fi
  done
}

# assert_all_calls_returned <json_file> [<json_file> ...]
#
# Validates that all 'call' events in AppMap JSON file(s) have corresponding 'return' events.
# Returns failure if any call IDs are missing their return events (orphaned calls).
# Supports multiple files as arguments.
assert_all_calls_returned() {
  local has_errors=0

  for json_file in "$@"; do
    if [ ! -f "$json_file" ]; then
      echo "File not found: $json_file"
      has_errors=1
      continue
    fi

    # Extract IDs that exist as 'call' but not as a 'return' parent_id
    local orphans
    orphans=$(jq -e -r '.events |
        (map(select(.event == "call").id) // []) as $calls |
        (map(select(.event == "return").parent_id) // []) as $returns |
        ($calls - $returns)[]
    ' "$json_file" 2>/dev/null)

    # If orphans is not empty, print them and mark as error
    if [[ -n "$orphans" ]]; then
      echo "Validation Failed: $json_file"
      echo "The following call IDs are missing a return event:"
      echo "$orphans"
      has_errors=1
    fi
  done

  return $has_errors
}

# requires_oracle
#
# Skips the current test if ORACLE_URL environment variable is not set.
# Used to conditionally run Oracle-specific tests.
requires_oracle() {
  if [ -z "$ORACLE_URL" ]; then
    skip "ORACLE_URL is not set"
  fi
}
