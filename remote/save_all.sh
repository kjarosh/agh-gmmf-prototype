#!/bin/bash
set -e

for host in $(cat ./hosts.lst); do
  echo Executing save on "${host}" ...
  ssh "${host}" 'cd /home/ubuntu/kjarosz && ./save.sh' | sed -e "s/^/[${host}] /" &
done

wait
