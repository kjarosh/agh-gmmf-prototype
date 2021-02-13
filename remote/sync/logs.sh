#!/bin/bash
set -e

zone_id=$(./zone_id.sh)
container_name=kjarosz_sim_${zone_id}

docker logs "$@" "$container_name"
