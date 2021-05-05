#!/bin/bash

####################
# Global constants #
####################

# kubernetes
path_to_kubernetes_config="$HOME/.kube/student-k8s-cyf.yaml"
kubernetes_user_name=$(kubectl config view --minify | grep namespace: | cut -d':' -f2)

# constant paths
sql_py_scripts="sql-and-python"
results_root="tests--artifacts-and-results/kubernetes"

# file names
test_config_path=${1:-'test-config.txt'}
echo -n "Config is: "
echo $test_config_path
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
path_for_test=""
path_for_graph=""
path_for_load=""
path_for_repetition=""

path_to_merged_csv=""
path_to_plot=""
path_to_report=""
path_to_average_report=""

path_to_graph=""
path_to_queries=""

# pods
COUNT_ZONES=0
ZONES=()
EXECUTOR=""

# number of repetition
TEST_TIME=0
REPETITIONS=0

####################
# Graphs and loads #
####################

# interzone levels
inter_zone_levels=()

# nodes per zone
nodes_per_zone=()

# data points - X coordinates
loads=()

#############
# Functions #
#############

my_printf() {
  echo ""
  echo -n "[PMarshall Script] "
  echo "${1}"
  echo ""
}

parse_config() {
  counter=0
  while read -r line; do
    case ${counter} in
    0)
      COUNT_ZONES=${line}
      ;;
    1)
      inter_zone_levels+=("${line}")
      ;;
    2)
      nodes_per_zone+=("${line}")
      ;;
    3)
      IFS=' ' read -r -a loads <<< "${line}"
      ;;
    4)
      TEST_TIME=${line}
      ;;
    5)
      REPETITIONS=${line}
      ;;
    esac
    ((counter++))
  done <"${test_config_path}"
}

# ZONES ON KUBERNETES

create_zones() {
  total_zones=$((COUNT_ZONES + 1))
  ./run-main.sh com.github.kjarosh.agh.pp.cli.KubernetesClient -z ${total_zones} -c ${path_to_kubernetes_config} -n ${kubernetes_user_name} -i danieljodlos/gmm-indexer-fork

  # local variables
  local incomings=1
  local pendings=1
  local creatings=1
  local runnings=0

  # loop
  while [[ ${incomings} -gt 0 || ${runnings} -ne ${total_zones} ]]; do
    # sleep
    sleep 5

    # check status of pods that will be run later
    pendings="$(kubectl get pod | grep -w Pending | wc -l)"
    creatings="$(kubectl get pod | grep -w ContainerCreating | wc -l)"
    incomings="$((pendings+creatings))"

    # check number of pods that are running now
    runnings="$(kubectl get pod | grep -w Running | wc -l)"
  done
}

load_zones() {
#  for ((i = 0; i < COUNT_ZONES; i++)); do
#    ZONES+=("$(kubectl get pod --field-selector=status.phase=Running -l "zone=zone${i}" -o name | sed 's/.*\///')")
#  done
#
#  EXECUTOR="$(kubectl get pod --field-selector=status.phase=Running -l "zone=zone${COUNT_ZONES}" -o name | sed 's/.*\///')"

  for ((i = 0; i < COUNT_ZONES; i++)); do
    newZone="$(kubectl get pod -l "zone=zone${i}" | grep -w Running | awk '{print $1;}')"
    my_printf "zone-${i} = ${newZone}"
    ZONES+=("${newZone}")
  done

  EXECUTOR="$(kubectl get pod -l "zone=zone${COUNT_ZONES}" | grep -w Running | awk '{print $1;}')"
  my_printf "executor = ${EXECUTOR}"
}

# NEW DIRECTORIES

mkdir_for_whole_test() {
  # create new directory
  timestamp=$(date +"D%Y-%m-%dT%T" | tr : -)
  path_for_test="${results_root}/test--${timestamp}"
  mkdir "${path_for_test}"

  # prepare paths
  path_to_merged_csv="${path_for_test}/${merged_csv_name}"
  path_to_plot="${path_for_test}/${plot_name}"

  # copy config
  cp ${test_config_path} "${path_for_test}/test-config.txt"

  # create merged_csv file
  touch "${path_to_merged_csv}"
  echo 'interzone,nodes_per_zone,target,real' >"${path_to_merged_csv}"
}

mkdir_for_graph() {
  # remove file extension from name of the directory
  path_for_graph="${path_for_test}/graph--${1}-${2}"
  mkdir "${path_for_graph}"
}

mkdir_for_load() {
  path_for_load="${path_for_graph}/${1}"
  mkdir "${path_for_load}"

  path_to_average_report="${path_for_load}/${avg_report_name}"
  touch "${path_to_average_report}"
}

mkdir_for_repetition() {
  path_for_repetition="${path_for_load}/${1}"
  mkdir "${path_for_repetition}"

  path_to_report="${path_for_repetition}/${report_name}"
  touch "${path_to_report}"
}

# GENERATE GRAPHS AND QUERIES

generate_graph() {
  # $1 - interzone
  # $2 - npz

  path_to_graph="${path_for_graph}/${graph_name}"

  # path to graph-generation config
  pgc="${path_for_graph}/graph-config.json"
  touch "${pgc}"
  echo "{" >> "${pgc}"
  echo "  \"providers\": 400," >> "${pgc}"
  echo "  \"spaces\": 600," >> "${pgc}"
  echo "  \"zones\": ${COUNT_ZONES}," >> "${pgc}"
  echo "  \"providersPerSpace\": \"normal(1, 0.5)\"," >> "${pgc}"
  echo "  \"groupsPerGroup\": \"normal(1.8, 1)\"," >> "${pgc}"
  echo "  \"usersPerGroup\": \"normal(9, 4)\"," >> "${pgc}"
  echo "  \"treeDepth\": \"enormal(2.3, 1)\"," >> "${pgc}"
  echo "  \"differentGroupZoneProb\": 0.${1}," >> "${pgc}"
  echo "  \"differentUserZoneProb\": 0.${1}," >> "${pgc}"
  echo "  \"existingUserProb\": 0.25," >> "${pgc}"
  echo "  \"existingGroupProb\": 0.1" >> "${pgc}"
  echo "}" >> "${pgc}"

  ./run-main.sh com.github.kjarosh.agh.pp.cli.GraphGeneratorMain -c ${pgc} -o ${path_to_graph} -n ${2}
}

generate_queries() {
  path_to_queries="${path_for_graph}/${queries_name}"

  max=${loads[0]}
  for n in "${loads[@]}" ; do
      ((n > max)) && max=$n
  done

  total_operations=$((TEST_TIME * max * 12 / 10))

  ./run-main.sh com.github.kjarosh.agh.pp.cli.OperationSequenceGeneratorMain -g ${path_to_graph} -n ${total_operations} -o ${path_to_queries}
}

# INSTRUMENTATION

clear_instrumentation() {
  kubectl exec -it "$1" -- touch temp.csv
  kubectl exec -it "$1" -- cp temp.csv instrumentation.csv
  kubectl exec -it "$1" -- rm temp.csv
}

clear_instrumentations() {
  for zone in ${ZONES[*]}; do
    clear_instrumentation "${zone}"
  done
}

get_instrumentation() {
  kubectl cp "${ZONES[$1]}":instrumentation.csv "${path_for_repetition}/instrumentation-$1.csv"
}

get_all_instrumentations() {
  for ((i = 0; i < COUNT_ZONES; i++)); do
    get_instrumentation "${i}"
  done

  my_printf "Artifacts downloaded"
}

# REDIS

clear_redis() {
  kubectl exec -it "$1" -- redis-cli FLUSHALL
}

clear_redises() {
  my_printf "Clearing redis"

  for ((i = 0; i < COUNT_ZONES; i++)); do
    clear_redis "${ZONES[i]}"
  done
}

# POSTGRES

database() {
  psql "dbname='postgres'" -f "$1"
}

postgres_clear() {
  database ${sql_py_scripts}/truncate.sql
}

postgres_report() {
  database ${sql_py_scripts}/get_report.sql > "${path_to_report}" 2>&1
}

postgres_import() {
  ./run-main.sh com.github.kjarosh.agh.pp.cli.PostgresImportMain ${path_for_repetition}
}

calculate_avg_report() {
  # $1 = folder z wynikami testu
  echo "TODO"
  result=$(python3 sql-and-python/calculate_avg_report.py $1)
  echo $result >> ${path_to_merged_csv}
}

# CONSTANT LOAD

load_graph() {
  clear_redises
  kubectl cp "${path_to_graph}" "${EXECUTOR}:${graph_name}"
  kubectl cp "${path_to_queries}" "${EXECUTOR}:${queries_name}"

  kubectl exec -it "${EXECUTOR}" -- bash \
            -c "./run-main.sh com.github.kjarosh.agh.pp.cli.ConstantLoadClientMain -l -b 5 -g ${graph_name} -n 100 -d 0 -t"


  # make sure there is a backup
  kubectl exec "${EXECUTOR}" -- redis-cli save
  kubectl exec "${EXECUTOR}" -- cp -p /var/lib/redis/dump.rdb /var/lib/redis/graph.rdb
}

constant_load() {
  # $1 - graph
  # $2 - queries
  # $3 - load
  # $4 - naive

  if [[ ${4} = true ]] ; then
    kubectl exec -it "${EXECUTOR}" -- bash \
            -c "./run-main.sh com.github.kjarosh.agh.pp.cli.ConstantLoadClientMain -b 5 -g ${graph_name} -s ${queries_name} -n ${3} -d ${TEST_TIME} -t 3 --disable-indexation"
  else
    kubectl exec -it "${EXECUTOR}" -- bash \
            -c "./run-main.sh com.github.kjarosh.agh.pp.cli.ConstantLoadClientMain -b 5 -g ${graph_name} -s ${queries_name} -n ${3} -d ${TEST_TIME} -t 3"
  fi

  # restore previous redis state
  kubectl exec "${EXECUTOR}" -- redis-cli shutdown
  kubectl exec "${EXECUTOR}" -- cp -p /var/lib/redis/graph.rdb /var/lub/redis/dump.rdb
  kubectl exec "${EXECUTOR}" -- redis-server /redis.conf
}

######################
# Single-test runner #
######################

run_test() {
  # $1 - graph
  # $2 - queries
  # $3 - load
  # $4 - naive (true/false)

  # clear csv and postgres
  clear_instrumentations
  postgres_clear
  my_printf "Postgres: CLEARED"

  # perform test
  constant_load "${1}" "${2}" "${3}" "${4}"

  # load results to postgres
  get_all_instrumentations
  postgres_import
  my_printf "Postgres: IMPORTED"

  # perform report and write final results to '${merged_csv_name}'
  postgres_report
  my_printf "Postgres: REPORT OBTAINED"
}

####################
# SCRIPT EXECUTION #
####################
service postgresql start
runuser -l postgres -c 'cd / && createuser --superuser root && createdb root'
service postgresql restart >> logs.txt
psql -f ${sql_py_scripts}/set_postgres_passwd.sql >> logs.txt 
service postgresql restart >> logs.txt

# read config file
parse_config

# create pods and obtain references to them
# create_zones
load_zones

## initialize new directory for test's results, including merged_csv file
mkdir_for_whole_test

# for each interzone..
for interzone_arg in ${inter_zone_levels[*]}; do

  # if interzone is 'naive' default it to 10%
  if [[ ${interzone_arg} = "naive" ]] ; then
    interzone=10
  else
    interzone=${interzone_arg}
  fi

  # for each nodes-per-zone..
  for npz_arg in ${nodes_per_zone[*]}; do
    mkdir_for_graph "${interzone_arg}" "${npz_arg}"

    # if nodes-per-zone is 'naive' default it to 20k
    if [[ ${npz_arg} = "naive" ]] ; then
      npz=20000
    else
      npz=${npz_arg}
    fi

    naive=false
    if [[ ${interzone_arg} = "naive" || ${npz_arg} = "naive" ]] ; then
      naive=true
    fi

    # generate graph and queries to perform
    generate_graph "${interzone}" "${npz}"
    generate_queries "${path_to_graph}"

    # load graph to kubernetes
    load_graph

    # for each load..
    for load in ${loads[*]}; do
      mkdir_for_load "${load}"

      # start new record in merged csv
      echo -n "${interzone_arg},${npz_arg},${load}," >> "${path_to_merged_csv}"

      # repeat test
      for i in $(seq 1 $REPETITIONS); do
        mkdir_for_repetition "${i}"
        run_test "${graph_name}" "${queries_name}" "${load}" ${naive}
      done

      # TODO calculate average report
      #...

      # TODO get value from average report to merged csv
      #grep 'Operations per second' "${path_to_average_report}" | cut -d':' -f6 | tr -d ' ' | head -n 1 >> "${path_to_merged_csv}"
      # echo "1" >> "${path_to_merged_csv}"

      echo -n "${interzone_arg},${load}," >> ${path_to_merged_csv}

      calculate_avg_report ${path_for_load}
    done

    # create plot from merged.csv
    python3 -W ignore "${sql_py_scripts}/plot.py" "${path_to_merged_csv}" "${path_to_plot}"

  done

done

echo "Tests finished!"

### infinite loop waiting for manual termination
while :
do
    echo "Awaiting termination. Stuck in infinite loop"
    sleep 10
done
