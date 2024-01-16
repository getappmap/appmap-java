#!/usr/bin/env bash

# This file is meant to contain any code required to use helper.bash functions
# in a bashunit script.

assert_equal() {
  [[ ! -z "$3" ]] && assert_equals "$2" "$1" "$3" || assert_equals "$2" "$1"
}
