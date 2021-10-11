#!/bin/bash
set -e

for host in $(cat ./hosts.lst); do
  echo Gathering logs from "${host}" ...
  ssh "${host}" 'cd /home/ubuntu/kjarosz && ./logs.sh -f' | sed -e "s/^/[${host}] /" &
done

wait
