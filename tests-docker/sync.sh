#!/bin/bash
set -e

source=./sync/
destination=/home/ubuntu/kjarosz

for host in $(cat ./hosts.lst); do
  echo Synchronizing "${host}" ...
  rsync -ar ${source} "${host}":${destination} &
done

wait
