#!/bin/bash

redis-cli shutdown
cp -p /var/lib/redis/graph.rdb /var/lib/redis/dump.rdb
redis-server /redis.conf --port 6379 2>&1 | sed 's/^/[redis] /' >&2 &
