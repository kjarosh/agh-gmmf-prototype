# GMMF Prototype

A prototype of the Group Membership Management Framework.
Implemented according to the concepts presented in the article entitled
"Group Membership Management Framework for Decentralized Collaborative Systems".

The prototype simulates a P2P environment consisting of multiple so-called "zones",
which represent autonomous organizations in a decentralized network.

## Repository structure

* `config` &mdash; exemplary application configurations,
* `tests-docker` &mdash; scripts used to benchmark the prototype on bare Docker
  (customized for the specific setup used in the article),
* `tests-k8s` &mdash; scripts and configuration used to benchmark the prototype in Kubernetes,
* `util` &mdash; utility scripts,
* `gmmf-*` &mdash; the application source code divided into modules,
* `gmmf-zone-simulator` &mdash; the main module which represents the zone,
* `docker-compose.yml` &mdash; Compose configuration with an exemplary 3-node setup,
* `docker-compose.util.yml` &mdash; Compose configuration for utility images (neo4j, Postgres, etc.).

## Main classes

* `ConstantLoadClientMain` &mdash; a client used to generate a constant load on specified servers,
* `GraphGeneratorMain` &mdash; graph generator,
* `KubernetesClient` &mdash; a Kubernetes client used for management of the application,
* `Neo4jImportMain` &mdash; imports the graph into Neo4j,
* `OperationSequenceGeneratorMain` &mdash; generator for sequences of operations used for benchmarking,
* `PostgresImportMain` &mdash; imports instrumentation data into Postgres,
* `QueryClientMain` &mdash; runs queries on specified servers (used for benchmarking),
* `QuerySequenceGeneratorMain` &mdash; generates membership queries (used for benchmarking).
