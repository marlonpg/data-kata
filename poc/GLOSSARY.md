# Glossary (Non-obvious Terms from README)

## A
- **API**: Application Programming Interface; endpoints another system can call.
- **Avro**: Data serialization format often used with Kafka and schema evolution.

## B
- **Backfill**: Reprocessing historical data to fill gaps or rebuild outputs.

## C
- **Canonical topic**: A normalized Kafka topic with a standard schema from multiple sources.
- **CDC (Change Data Capture)**: Capturing row-level database changes (insert/update/delete) as events.
- **Compatibility policy (schema)**: Rules controlling whether schema changes are allowed.

## D
- **DAG**: Directed acyclic graph, commonly used to represent workflow steps and dependencies.
- **Dead-letter topic (DLT/DLQ)**: Kafka topic where invalid/failed messages are sent for review.

## E
- **ELK**: Elasticsearch, Logstash, Kibana stack for logs.
- **EOS (Exactly Once Semantics)**: Processing guarantee aiming to avoid duplicates and losses.

## F
- **FilePulse**: Kafka Connect plugin specialized for file ingestion and parsing.

## I
- **Idempotent producer**: Kafka producer mode that prevents duplicate record writes on retries.
- **Immutable storage**: Storage where data is not changed in place, helpful for replay and auditing.
- **Incremental mode**: Reading only new/changed data rather than full snapshots each run.

## J
- **JAX-WS**: Java API for building SOAP web services clients/servers.
- **JDBC Source Connector**: Kafka Connect connector that reads from relational databases via JDBC.

## K
- **Kafka Connect**: Framework for running source/sink data connectors for Kafka.
- **Kafka Streams**: Java library for building stream processing applications on Kafka.

## L
- **Lineage**: Metadata about where data came from, how it was transformed, and where it went.

## M
- **Marquez**: Open-source lineage metadata service and UI.
- **MVP**: Minimum Viable Product; smallest useful deliverable.

## N
- **NFS**: Network File System; shared file storage over network.

## O
- **Observability**: Ability to inspect system behavior via metrics, logs, traces.
- **OpenLineage**: Open standard/event model for lineage metadata.
- **OpenTelemetry**: Standard for telemetry (traces/metrics/log context).
- **OpenSearch**: Search and analytics engine often used for log indexing.

## P
- **Parquet**: Columnar file format optimized for analytics workloads.
- **Partitioning strategy**: Rule for distributing events across Kafka partitions.
- **PoC**: Proof of Concept.
- **Prometheus**: Time-series monitoring system for metrics.
- **Protobuf**: Compact binary serialization format with schema support.

## R
- **Replay strategy**: Controlled way to re-read historical data and recompute outputs.

## S
- **Schema Registry**: Service that stores and validates data schemas for producers/consumers.
- **SLO**: Service Level Objective; measurable reliability target.
- **SOAP / WS-***: XML-based web service protocols used in legacy enterprise systems.

## T
- **Topology (Kafka Streams)**: Graph of stream/table operations in a Streams app.

## W
- **Windowed aggregation**: Aggregation over a time window (for example daily/monthly buckets).
