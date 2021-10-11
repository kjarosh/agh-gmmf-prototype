#!/bin/bash
set -e

image=${1:-kjarosh/agh-gmmf-prototype:latest}
pull=${2:-true}

zone_id=$(./zone_id.sh)
ip_addr=$(./ip_addr.sh)
container_name=kjarosz_sim_${zone_id}

if docker stop "${container_name}" >/dev/null 2>&1; then
  echo "Container stopped";
else
  echo "Container is not running";
fi

if docker rm "${container_name}" >/dev/null 2>&1; then
  echo "Container removed"
else
  echo "Container does not exist"
fi

rm -rf artifacts || true
mkdir -p artifacts
if [ "$pull" == "true" ]; then
  docker pull "${image}"
fi
docker run -d \
    -p 30080:80 \
    -p 30087:8080 \
    -p 30091:30091 \
    -e "ZONE_ID=${zone_id}" \
    -e "JMX_HOST=${ip_addr}" \
    -e "JMX_PORT=30091" \
    -v "$(pwd)/config.json:/config/config.json" \
    -v "$(pwd)/artifacts:/artifacts" \
    --name "${container_name}" \
    "${image}" server
