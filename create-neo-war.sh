#!/bin/bash

# https://blog.yossarian.net/2020/01/23/Anybody-can-write-good-bash-with-a-little-effort
# https://sipb.mit.edu/doc/safe-shell/
set -euo pipefail

function main {
  wabPath=target/ping.jar
  [[ -f $wabPath ]] || die "$wabPath doesnt exist"
  warFilename=$(basename $wabPath)
  warName="${warFilename%.*}.war"
  warPath=target/$warName
  echo "Wrapping $wabPath in $warPath"
  zip -j $warPath $wabPath src/main/resources/deploy.json

}

function check_deps {
  echo "-- check_deps"
  # local deps=(curl cf)
  # for dep in "${deps[@]}"; do
  #   installed "${dep}" || die "MISSING '${dep}'"
  # done
}

function installed {
  cmd=$(command -v "${1}")

  [[ -n "${cmd}" ]] && [[ -f "${cmd}" ]]
  return ${?}
}

function die {
  >&2 echo "Fatal: ${@}"
  exit 1
}

main
