#!/bin/bash

COUNT_ZONES=${1}
ZONES=()

load_zones() {
  for ((i = 0; i < COUNT_ZONES; i++)); do
    newZone="$(kubectl get pod -l "zone=zone${i}" | grep -w Running | awk '{print $1;}')"
    ZONES+=("${newZone}")
  done
}

clear_redis() {
  kubectl exec -it "$1" -- redis-cli FLUSHALL
}

clear_redises() {
  for ((i = 0; i < COUNT_ZONES; i++)); do
    clear_redis "${ZONES[i]}"
  done
}

load_zones
clear_redises

kubectl cp ./graph.json ${ZONES[COUNT_ZONES - 1]}:/graph.json
kubectl cp ./queries_caller.sh ${ZONES[COUNT_ZONES - 1]}:/queries_caller.sh
kubectl exec -it "${ZONES[COUNT_ZONES - 1]}" -- bash -c "chmod 777 ./queries_caller.sh"
kubectl exec -it "${ZONES[COUNT_ZONES - 1]}" -- bash -c "./queries_caller.sh"