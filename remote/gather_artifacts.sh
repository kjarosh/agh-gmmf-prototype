#!/bin/bash
set -e

destination=./artifacts

for host in $(cat ./hosts.lst); do
  echo Gathering artifacts from "${host}" ...
  mkdir -p "$destination/$host"
  rsync -ar "${host}":/home/ubuntu/kjarosz/artifacts/ "$destination/$host/" &
done

wait
