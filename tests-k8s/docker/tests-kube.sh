#!/bin/bash
set -eE -o functrace

failure() {
  local lineno=$1
  local msg=$2
  echo "Failed at $lineno: $msg"
  while true; do sleep 100; done
}
trap 'failure ${LINENO} "$BASH_COMMAND"' ERR

####################
# Global constants #
####################

# kubernetes
KUBECONFIG="kubeconfig.yaml"
k8s_namespace=$(cat /var/run/secrets/kubernetes.io/serviceaccount/namespace)

# constant paths
results_root="results"

mkdir -p ${results_root}

# file names
test_config_path=${1:-'configs/test-config.conf'}
merged_csv_name="merged.csv"
graph_name="graph.json"
queries_name="queries.json"
plot_name="plot.png"
report_name="report.txt"
avg_report_name="avg-report.txt"

####################
# Global variables #
####################

# paths (yet to be determined) to navigate over results
test_dir_path=""
graph_dir_path=""
load_dir_path=""
repetition_dir_path=""

merged_csv_path=""
plot_path=""
report_path=""
avg_report_path=""

graph_file_path=""
queries_file_path=""

# pods
COUNT_ZONES=0
ZONES=()

WARMUP_TIME=0
TEST_TIME=0
REPETITIONS=0

####################
# Graphs and loads #
####################

# inter-zone levels
inter_zone_levels=()

# nodes per zone
spaces_per_zone=()

# data points - X coordinates
loads=()

#############
# Functions #
#############

my_printf() {
  printf "[tests-kube] %s\n" "$1"
}

init_kubeconfig() {
  cat <<CONFIG > "${KUBECONFIG}"
apiVersion: v1
clusters:
- cluster:
    insecure-skip-tls-verify: true
    server: https://nsdev.onedata.uk.to:6443
  name: k8s-cyfronet-1
contexts:
- context:
    cluster: k8s-cyfronet-1
    namespace: ${k8s_namespace}
    user: k8s-cyfronet-1-user
  name: student-k8s-cyf.yaml
current-context: student-k8s-cyf.yaml
kind: Config
preferences:
  colors: true
users:
- name: k8s-cyfronet-1-user
  user:
    token: student#student@gmail.com
CONFIG
}

parse_config() {
  echo "Config:"
  echo "========== $test_config_path"
  cat "${test_config_path}"
  echo "=========="
  source "${test_config_path}"
}

# ZONES ON KUBERNETES

create_zones() {
  ./run-main.sh com.github.kjarosh.agh.pp.cli.KubernetesClient \
    -z ${COUNT_ZONES} \
    -c ${KUBECONFIG} \
    -n ${k8s_namespace} \
    -i ${ZONE_IMAGE}

  my_printf "Zones created by KubernetesClient"
}

load_zones() {
  # wait for pods to be in 'Running' state
  local zones_ready=0
  my_printf "Waiting for zones' readiness"
  while [[ ${zones_ready} -eq 0 ]]; do
    sleep 5

    local incoming=$(kubectl get pod | grep ' \(ContainerCreating|Pending\) ' | wc -l)
    local running=$(kubectl get pod | grep "^zone[a-z0-9-]* *1/1 *Running" | wc -l)

    if [[ ${incoming} -eq 0 && ${running} -eq ${COUNT_ZONES} ]]; then
      zones_ready=1
    else
      my_printf "Zones not ready yet: incoming=${incoming}, running=${running}"
    fi
  done
  my_printf "Zones ready. Saving pods IDs"

  # save IDs of pods
  ZONES=()
  for ((i = 0; i < COUNT_ZONES; i++)); do
    local newZone="$(kubectl get pod -l "zone=zone${i}" -n "$k8s_namespace" | grep -w Running | awk '{print $1;}')"
    my_printf "zone-${i} = ${newZone}"
    ZONES+=("${newZone}")
  done
}

restart_zone() {
  my_printf "Restarting $1"
  kubectl -n "$k8s_namespace" rollout restart deployment "$1"
  kubectl -n "$k8s_namespace" rollout status deployment "$1"
  my_printf "Restarted $1"
}

restart_zones() {
  my_printf "Restarting zones"

  for ((i = 0; i < COUNT_ZONES; i++)); do
    restart_zone "zone${i}" &
  done
  wait

  load_zones

  my_printf "Zones restarted"
}

# NEW DIRECTORIES

mkdir_for_whole_test() {
  # create new directory
  timestamp=$(date +"D%Y-%m-%dT%T" | tr : -)
  test_dir_path="${results_root}/test--${timestamp}"
  mkdir "${test_dir_path}"

  # prepare paths
  merged_csv_path="${test_dir_path}/${merged_csv_name}"
  plot_path="${test_dir_path}/${plot_name}"

  # copy config
  cp "${test_config_path}" "${test_dir_path}/test-config.conf"

  # create merged_csv file
  touch "${merged_csv_path}"
  echo 'interzone,spaces_per_zone,target,real,std' >"${merged_csv_path}"
}

mkdir_for_graph() {
  # remove file extension from name of the directory
  graph_dir_path="${test_dir_path}/graph--${1}-${2}"
  mkdir "${graph_dir_path}"
}

mkdir_for_load() {
  load_dir_path="${graph_dir_path}/${1}"
  mkdir "${load_dir_path}"

  avg_report_path="${load_dir_path}/${avg_report_name}"
  touch "${avg_report_path}"
}

mkdir_for_repetition() {
  repetition_dir_path="${load_dir_path}/${1}"
  mkdir "${repetition_dir_path}"

  report_path="${repetition_dir_path}/${report_name}"
  touch "${report_path}"
}

# GENERATE GRAPHS AND QUERIES

generate_graph() {
  # $1 - inter-zone
  # $2 - spz

  graph_file_path="${graph_dir_path}/${graph_name}"

  if [[ -f "/init/graph.json" ]]; then
    cp "/init/graph.json" "${graph_file_path}"
    my_printf "Graph already generated, skipping"
    return
  fi

  # graph-generation config
  pgc="${graph_dir_path}/graph-config.json"

  # ASSUMPTION: 1 space pez zone 'generates' around 30 nodes per zone
  local inter_zone_param=${1}
  local spaces_param=${2}
  local providers_param=$((2 * spaces_param / 3))

  cat <<CONFIG > "${pgc}"
{
  "providers": ${providers_param},
  "spaces": ${spaces_param},
  "zones": 1,
  "providersPerSpace": "normal(1, 0.5)",
  "groupsPerGroup": "normal(1.8, 1)",
  "usersPerGroup": "normal(9, 4)",
  "treeDepth": "enormal(2.3, 1)",
  "differentGroupZoneProb": 0.${inter_zone_param},
  "differentUserZoneProb": 0.${inter_zone_param},
  "existingUserProb": 0.1,
  "existingGroupProb": 0.05
}
CONFIG

  echo "Generating graph..."
  ./run-main.sh com.github.kjarosh.agh.pp.cli.GraphGeneratorMain -c "${pgc}" -o "${graph_file_path}" -s ${COUNT_ZONES}
  echo "Graph generated"
}

generate_queries() {
  queries_file_path="${graph_dir_path}/${queries_name}"

  if [[ -f "/init/queries.json.gz" ]]; then
    gunzip -c "/init/queries.json.gz" > "${queries_file_path}"
    my_printf "Operations already generated, skipping"
    return
  fi

  max=${loads[0]}
  for n in "${loads[@]}" ; do
      ((n > max)) && max=$n
  done

  total_operations=$(((WARMUP_TIME + TEST_TIME) * max * 12 / 10))

  echo "Generating queries... n=${total_operations}"
  ./run-main.sh com.github.kjarosh.agh.pp.cli.OperationSequenceGeneratorMain \
                  -g ${graph_file_path} -n ${total_operations} -o ${queries_file_path}
  echo "Queries generated"
}

# INSTRUMENTATION

clear_instrumentation() {
  my_printf "Clearing instrumentation in $1"
  kubectl exec "$1" -- truncate -s 0 instrumentation.csv
}

clear_instrumentations() {
  for zone in ${ZONES[*]}; do
    clear_instrumentation "${zone}" &
  done
  wait

  my_printf "Instrumentation cleared"
}

get_instrumentation() {
  my_printf "Downloading artifacts for $1"
  kubectl exec "${ZONES[$1]}" -- gzip -k instrumentation.csv
  local count=0
  while ! kubectl cp "${ZONES[$1]}":instrumentation.csv.gz "${repetition_dir_path}/instrumentation-$1.csv.gz"; do
    count=$((count + 1))
    if [[ $count -gt 10 ]]; then
      return
    fi

    my_printf "Retrying download for $1"
    rm -f "${repetition_dir_path}/instrumentation-$1.csv"
  done

  gunzip -c "${repetition_dir_path}/instrumentation-$1.csv.gz" > "${repetition_dir_path}/instrumentation-$1.csv"
  rm "${repetition_dir_path}/instrumentation-$1.csv.gz"
}

get_all_instrumentations() {
  for ((i = 0; i < COUNT_ZONES; i++)); do
    get_instrumentation "${i}" &
  done
  wait

  my_printf "Artifacts downloaded"
}

# REDIS

clear_redis() {
  kubectl exec "$1" -- redis-cli FLUSHALL
}

clear_redises() {
  my_printf "Clearing redis"

  for ((i = 0; i < COUNT_ZONES; i++)); do
    clear_redis "${ZONES[i]}" &
  done
  wait
}

# POSTGRES

database() {
  psql "dbname='postgres'" -f "$1"
}

postgres_clear() {
  database /dev/stdin <<PSQL
  delete from dbnotification;
PSQL
}

postgres_report() {
  database /dev/stdin <<PSQL > "${report_path}" 2>&1
do \$\$ begin
  perform report(
    (select (min(time) + interval '${WARMUP_TIME} second') from dbnotification),
    (select (min(time) + interval '$((WARMUP_TIME + TEST_TIME)) second') from dbnotification));
end \$\$
PSQL
  database /dev/stdin <<PSQL > "${repetition_dir_path}/time_plot_5sec.txt" 2>&1
do \$\$ declare
	t timestamp;
	u timestamp;
begin
	t := (select min(time) from dbnotification);
	u := (select max(time) from dbnotification);
	perform time_plot(t, u, interval '5 seconds');
end \$\$
PSQL
}

postgres_import() {
  ./run-main.sh com.github.kjarosh.agh.pp.cli.PostgresImportMain ${repetition_dir_path}
}

time_to_seconds() {
  awk -F: '{ print ($1 * 3600) + ($2 * 60) + $3 }'
}

gather_dat() {
  results_dat="${test_dir_path}/results.dat"
  printf "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n" \
    "load" \
    "queued" \
    "throughput" \
    "queued_events" \
    "events" \
    "op_duration_avg" \
    "op_duration_max" \
    "event_duration_avg" \
    "event_duration_max" \
    "event_queue_wait_avg" \
    "event_queue_wait_max" > "$results_dat"

  for load in $(find "${graph_dir_path}" -maxdepth 1 -mindepth 1 -type d -printf "%f\n" | sort -h); do
    report="${graph_dir_path}/$load/1/report.txt"
    queued=$(sed -n -e 17p "$report" | cut -d':' -f6)
    throughput=$(sed -n -e 16p "$report" | cut -d':' -f6)
    queued_events=$(sed -n -e 23p "$report" | cut -d':' -f6)
    events=$(sed -n -e 22p "$report" | cut -d':' -f6)
    op_duration_avg=$(sed -n -e 26p "$report" | cut -c 36- | time_to_seconds)
    op_duration_max=$(sed -n -e 27p "$report" | cut -c 36- | time_to_seconds)
    event_duration_avg=$(sed -n -e 34p "$report" | cut -c 36- | time_to_seconds)
    event_duration_max=$(sed -n -e 35p "$report" | cut -c 36- | time_to_seconds)
    event_queue_wait_avg=$(sed -n -e 30p "$report" | cut -c 36- | time_to_seconds)
    event_queue_wait_max=$(sed -n -e 31p "$report" | cut -c 36- | time_to_seconds)
    printf "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n" \
      "$load" \
      "$queued" \
      "$throughput" \
      "$queued_events" \
      "$events" \
      "$op_duration_avg" \
      "$op_duration_max" \
      "$event_duration_avg" \
      "$event_duration_max" \
      "$event_queue_wait_avg" \
      "$event_queue_wait_max" >> "$results_dat"
  done
}

# CONSTANT LOAD

load_graph() {
  restart_zones
  clear_redises

  ./run-main.sh com.github.kjarosh.agh.pp.cli.ConstantLoadClientMain -l -r 5 -g "${graph_file_path}" -n 100 --no-load

  # make sure there is a backup
  for ((i = 0; i < COUNT_ZONES; i++)); do
    (
      kubectl cp /save-redis-dump.sh "${ZONES[i]}:/save-redis-dump.sh"
      kubectl exec "${ZONES[i]}" -- sh /save-redis-dump.sh
    ) &
    my_printf "Redis state snapshoted for ${ZONES[i]}"
  done
  wait
}

constant_load() {
  # $1 - load
  # $2 - naive

  local additional_opts=""
  if [[ "${2}" = true ]] ; then
    additional_opts="--disable-indexation"
  fi

  ./run-main.sh com.github.kjarosh.agh.pp.cli.ConstantLoadClientMain \
                  -r 5 -g ${graph_file_path} -s ${queries_file_path} -n "${1}" \
                  -d $((WARMUP_TIME + TEST_TIME + 5)) -t 10 ${additional_opts}

  # restore previous redis state
  for ((i = 0; i < COUNT_ZONES; i++)); do
    kubectl cp /load-redis-dump.sh "${ZONES[i]}:/load-redis-dump.sh"
    kubectl exec "${ZONES[i]}" -- sh /load-redis-dump.sh
    my_printf "Redis state restored for ZONE ${ZONES[i]}"
  done
}

######################
# Single-test runner #
######################

run_test() {
  # $1 - load
  # $2 - naive (true/false)

  restart_zones

  # clear csv and postgres
  clear_instrumentations
  postgres_clear
  my_printf "Postgres: CLEARED"

  # perform test
  constant_load "${1}" "${2}"

  # wait a bit for instrumentation
  sleep 5

  # load results to postgres
  get_all_instrumentations
  postgres_import
  my_printf "Postgres: IMPORTED"

  # perform report and write final results to '${merged_csv_name}'
  postgres_report
  my_printf "Postgres: REPORT OBTAINED"

  gather_dat
  my_printf "Report generated"
}

####################
# SCRIPT EXECUTION #
####################
init_kubeconfig

service postgresql start
runuser -l postgres -c 'cd / && createuser --superuser root && createdb root'
service postgresql restart
psql -f sql/set_postgres_passwd.sql
service postgresql restart

# read config file
parse_config
my_printf "Config read"

# create pods and obtain references to them
create_zones
load_zones
my_printf "Zones loaded"

## initialize new directory for test's results, including merged_csv file
mkdir_for_whole_test
my_printf "Directory structure created"

# for each inter-zone lvl..
for inter_zone_arg in ${inter_zone_levels[*]}; do

  # if inter-zone is 'naive' then default it to 10%
  if [[ ${inter_zone_arg} = "naive" ]] ; then
    inter_zone_lvl=10
  else
    inter_zone_lvl=${inter_zone_arg}
  fi

  # for each spaces-per-zone value..
  for spz_arg in ${spaces_per_zone[*]}; do
    mkdir_for_graph "${inter_zone_arg}" "${spz_arg}"

    # if spaces-per-zone is 'naive' default it to 66 ( 2k nodes-per-zone )
    if [[ ${spz_arg} = "naive" ]] ; then
      spz=66
    else
      spz=${spz_arg}
    fi

    naive=false
    if [[ ${inter_zone_arg} = "naive" || ${spz_arg} = "naive" ]] ; then
      naive=true
    fi

    # generate graph and queries to perform
    generate_graph "${inter_zone_lvl}" "${spz}"
    generate_queries "${graph_file_path}"

    # load graph to kubernetes
    load_graph

    # for each load..
    for load in ${loads[*]}; do
      mkdir_for_load "${load}"

      # start new record in merged csv
      echo -n "${inter_zone_arg},${spz_arg},${load}," >> "${merged_csv_path}"

      # repeat test
      for i in $(seq 1 $REPETITIONS); do
        mkdir_for_repetition "${i}"
        run_test "${load}" ${naive}
      done
    done
  done
done

### delete zones
for ((i = 0; i < COUNT_ZONES; i++)); do
  kubectl delete deployment zone${i}
done

### prepare results to be downloaded
tester_pod=$(kubectl get pod | grep '^gmm-tester-[a-z0-9]* *1/1 *Running' | awk '{print $1;}')
mkdir -p /download-results
tar -czvf /download-results/results.tar.gz "${test_dir_path}"
find "${test_dir_path}" -size -4096c | tar -czvf /download-results/results-small.tar.gz -T -
md5sum /download-results/*
echo "Tar generated. Copy with:"
echo "kubectl -n ${k8s_namespace} cp $tester_pod:download-results/results.tar.gz results.tar.gz"
echo "devspace sync --namespace=${k8s_namespace} --pod=$tester_pod --container-path=download-results --download-only --no-watch"

echo "Tests finished!"

### infinite loop waiting for manual termination
while :
do
    sleep 100
done
