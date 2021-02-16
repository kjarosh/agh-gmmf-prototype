#!/bin/bash
set -e

for host in $(cat ./hosts.lst); do
  echo Executing run on "${host}" ...
  ssh "${host}" 'cd /home/ubuntu/kjarosz && ./run.sh kjarosh/ms-graph-simulator:saved false' | sed -e "s/^/[${host}] /" &
done

wait
