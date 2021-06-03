#!/bin/bash
./run-main.sh com.github.kjarosh.agh.pp.cli.QuerySequenceGeneratorMain -t members -g graph.json -n 200000 -o queries_members.json -e 1
./run-main.sh com.github.kjarosh.agh.pp.cli.QuerySequenceGeneratorMain -t reaches -g graph.json -n 200000 -o queries_reaches_exist.json -e 1
./run-main.sh com.github.kjarosh.agh.pp.cli.QuerySequenceGeneratorMain -t reaches -g graph.json -n 200000 -o queries_reaches_nonexist.json -e 0
./run-main.sh com.github.kjarosh.agh.pp.cli.QuerySequenceGeneratorMain -t ep -g graph.json -n 200000 -o queries_ep.json -e 1
./run-main.sh com.github.kjarosh.agh.pp.cli.ConstantLoadClientMain -b 5 -n 0 -t 3 -d 1 -g graph.json -l
for i in $(seq 1 5); do
    ./run-main.sh com.github.kjarosh.agh.pp.cli.QueryClientMain -g graph.json -d 300 -s queries_members.json -r queriesResults.json
    ./run-main.sh com.github.kjarosh.agh.pp.cli.QueryClientMain -g graph.json -d 300 -s queries_members.json --naive -r queriesResults.json
    ./run-main.sh com.github.kjarosh.agh.pp.cli.QueryClientMain -g graph.json -d 300 -s queries_reaches_exist.json -r queriesResults.json -l exist
    ./run-main.sh com.github.kjarosh.agh.pp.cli.QueryClientMain -g graph.json -d 300 -s queries_reaches_exist.json --naive -r queriesResults.json -l exist
    ./run-main.sh com.github.kjarosh.agh.pp.cli.QueryClientMain -g graph.json -d 300 -s queries_reaches_nonexist.json -r queriesResults.json -l nonexist
    ./run-main.sh com.github.kjarosh.agh.pp.cli.QueryClientMain -g graph.json -d 300 -s queries_reaches_nonexist.json --naive -r queriesResults.json -l nonexist
    ./run-main.sh com.github.kjarosh.agh.pp.cli.QueryClientMain -g graph.json -d 300 -s queries_ep.json -r queriesResults.json
    ./run-main.sh com.github.kjarosh.agh.pp.cli.QueryClientMain -g graph.json -d 300 -s queries_ep.json --naive -r queriesResults.json
done
./run-main.sh com.github.kjarosh.agh.pp.cli.QueriesAveragesLoggerMain -r queriesResults.json > results.txt
