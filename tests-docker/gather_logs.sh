#!/bin/bash
set -e

destination=./logs
mkdir -p "$destination"

for host in $(cat ./hosts.lst); do
  echo Gathering logs from "${host}" ...
  ssh "${host}" 'cd /home/ubuntu/kjarosz && ./logs.sh' > "$destination/$host.log" &
done

wait
