#!/bin/bash

redis-cli save
cp -p /var/lib/redis/dump.rdb /var/lib/redis/graph.rdb
