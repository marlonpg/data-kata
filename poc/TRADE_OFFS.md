# Trade-offs and Decision Notes

**Hard constraints (from requirements):**
- Must ingest from 3 source types: Relational DB, File system, WS-* (SOAP).
- Must use Spark, Flink, or Kafka Streams for processing.
- Must deliver 2 pipelines: Top Sales per City + Top Salesman nationally.
- Final aggregates in a dedicated DB exposed via REST API.
- **Restrictions: no Python, no Redshift, no Hadoop.**

**Evaluation criteria applied across all decisions:**
1. **Operational complexity** — how much infrastructure and expertise to run.
2. **Time-to-deliver** — speed to a working PoC.
3. **Constraint alignment** — compatibility with the Java-only, no-Python restriction.
4. **Use-case fit** — how well the tool solves our specific windowed aggregation + ranking problem.
5. **Production readiness path** — how hard it is to harden this choice for production later.

---

## 1. Stream Processing: Kafka Streams vs Apache Flink vs Spark Structured Streaming

**Our use case:** Two windowed aggregation pipelines — group sales by `(city_id, salesman_id)` and by `salesman_id` nationally, rank by total, sink to PostgreSQL. Input volume for PoC is low; production could be 10-100x.

### Kafka Streams (chosen)
**PROS (+)**
  * Integration: Native Kafka library — no separate cluster, no extra deployment. Runs as a plain JVM app alongside the existing Kafka broker.
  * Deployment: Single JAR per topology. Trivial to run in Docker or bare JVM with no cluster orchestration.
  * Latency: True record-at-a-time processing. Sub-second end-to-end latency for our aggregation windows.
  * Use-case fit: `KTable` + `groupBy` + `windowedBy` + `aggregate` maps directly to the "top sales per city" and "top salesman country" pipelines. Interactive Queries can serve results without a separate DB in future iterations.
  * Operations: Zero cluster management — scales horizontally by adding app instances (one per partition). For N partitions, up to N parallel instances.
  * EOS: Supports exactly-once semantics (EOS v2) out of the box with `processing.guarantee=exactly_once_v2`.

**CONS (-)**
  * Kafka lock-in: Processing is tightly coupled to Kafka as the transport. Migrating to a non-Kafka backbone would require rewriting topologies.
  * Scaling ceiling: Parallelism is bounded by partition count. For very high cardinality joins or large state, RocksDB state stores need careful tuning (changelog topics, compaction, disk I/O).
  * Complex patterns: Multi-stream joins across different time windows or session windows are more verbose than Flink's SQL/Table API equivalent.
  * No built-in SQL layer: Unlike Flink SQL, you must write Java DSL code for every transformation. Our two pipelines are simple enough that this is acceptable, but ad-hoc analytics queries are not possible without ksqlDB (separate component).

### Apache Flink
**PROS (+)**
  * State management: Savepoints, incremental checkpoints, and large-state handling are more mature than Kafka Streams' RocksDB backend.
  * Flink SQL: Our two aggregations could be expressed as pure SQL queries (`SELECT city_id, salesman_id, SUM(amount) ... GROUP BY TUMBLE(event_time, INTERVAL '1' MONTH), city_id`). Lower barrier for analysts to modify pipelines.
  * Unified batch/stream: Could run backfill (batch) and live processing (stream) with the same code.
  * Scaling: Scales independently of Kafka partition count via its own task slot model.

**CONS (-)**
  * Cluster overhead: Requires a dedicated Flink cluster (JobManager + TaskManagers). Even with Flink on YARN/K8s session mode, this adds significant operational complexity for a PoC.
  * Resource footprint: Minimum practical deployment is ~2-4 GB RAM for JobManager + TaskManagers. Overkill for our current low-volume PoC.
  * Deployment pipeline: Requires building fat JARs, submitting to the cluster, managing savepoints for upgrades. More ceremony than `java -jar streams.jar`.
  * Team familiarity: Additional learning curve if the team has no prior Flink experience.

### Spark Structured Streaming
**PROS (+)**
  * Unified platform: Batch + streaming in one runtime. If we later need batch backfills over historical files, Spark handles both.
  * Ecosystem: Broad connector support (Parquet, Delta Lake, Iceberg) and large community.

**CONS (-)**
  * Micro-batch latency: Default processing model is micro-batch (100ms-2s per trigger). Continuous processing mode exists but is still experimental and limited.
  * Constraint friction: PySpark is the dominant API — Java/Scala Spark is fully supported but most community examples, tutorials, and third-party integrations assume Python. This creates friction given our no-Python restriction.
  * Cluster requirement: Needs a Spark cluster (standalone, YARN, or K8s). Similar operational overhead to Flink with higher resource consumption.
  * Overkill: Spark shines at large-scale batch/ML workloads. For two real-time streaming aggregations, it's the heaviest option.

**Decision:** Kafka Streams. Lowest operational overhead, fastest time-to-PoC, natural fit for windowed aggregations on Kafka topics, and no additional infrastructure. If production data volumes exceed what a partitioned Kafka Streams app can handle (unlikely for sales data), Flink SQL would be the upgrade path — not Spark.

**Risk:** If the team later needs complex multi-stream temporal joins or ad-hoc SQL analytics on streams, Kafka Streams alone may not suffice. Mitigation: evaluate ksqlDB or Flink SQL in Phase 2.

---

## 2. Ingestion: Kafka Connect (hybrid) vs Fully Custom Services

**Our architecture uses a hybrid approach:** Kafka Connect for DB and file ingestion + a custom Java Kafka producer for SOAP. This section evaluates that hybrid against going fully custom.

### Kafka Connect + custom SOAP producer (chosen)
**PROS (+)**
  * Proven connectors: JDBC Source Connector (Confluent) is battle-tested for incremental/CDC ingestion from relational databases. The recommended mode is `timestamp+incrementing` against the source sales table.
  * Config-driven: Connector definitions are JSON templates — no code to write for DB and file sources. Portable across environments by swapping connection URLs.
  * Parallelism and offset management: Connect handles offset tracking, task distribution, and restarts natively. The JDBC connector remembers the last `updated_at` + `id` per table.
  * SOAP flexibility: A custom Java producer using JAX-WS/CXF can call WS-* endpoints with full control over WSDL binding, WS-Security headers, and retry logic — things no generic connector supports well.

**CONS (-)**
  * Plugin management: JDBC Source and FilePulse connectors are **not bundled** in the default `cp-kafka-connect` image. They must be installed manually (confluent-hub install or JAR copy). This is a deployment friction point.
  * FilePulse maturity: StreamThoughts FilePulse is a community connector, not Confluent-supported. Fewer production references and slower bug-fix cadence than Confluent-backed connectors. Alternative: Spooldir connector (more mature but less flexible).
  * Delivery semantics: `timestamp+incrementing` mode provides **at-least-once** delivery. If the connector restarts mid-poll, duplicate records may appear on the raw topic. The downstream Streams topology must handle deduplication (e.g., by `(id, updated_at)` composite key).
  * Debugging opacity: Kafka Connect error messages can be cryptic. Failed connectors surface in the REST API (`GET /connectors/status`) but root-cause diagnosis often requires reading Connect worker logs.
  * No schema enforcement by default: Out-of-the-box connector configurations use `JsonConverter` with no schema validation. Malformed records will flow through silently unless Schema Registry enforcement is added from the start. Phase 2 should switch to `AvroConverter` + Schema Registry.

### Fully custom ingestion microservices (not chosen)
**PROS (+)**
  * Full control: Custom error handling, retry policies, circuit breakers, and schema validation per source.
  * Unified codebase: All three ingestion paths would be Java microservices with the same build/deploy/monitor pattern.
  * Testability: Standard unit/integration tests. Connect connectors are harder to test outside a running Connect cluster.

**CONS (-)**
  * Dev effort: For DB polling alone, you'd re-implement offset tracking, incremental query building, timestamp handling, and restart recovery — all of which JDBC Source provides for free.
  * Maintenance surface: Three separate services to build, deploy, monitor, and upgrade vs two connector configs + one small producer.
  * Reinventing the wheel: The JDBC and file ingestion patterns are well-solved problems. Custom code here adds risk without adding value for the PoC.

**Decision:** Hybrid approach. Use Kafka Connect where mature connectors exist (JDBC, FilePulse) and custom Java code only where no connector fits (SOAP/WS-*). This minimizes code while keeping full flexibility for the hardest integration.

**Risks and mitigations:**
  * **FilePulse connector stability** — If FilePulse proves unreliable, fall back to the Spooldir connector or a minimal Java file-watcher producer.
  * **Duplicate records from JDBC** — Downstream Kafka Streams topology must implement idempotent processing keyed on `(id, updated_at)`.
  * **Plugin installation** — Document exact plugin versions and install steps in `STEPS.md`; consider building a custom Connect Docker image with plugins pre-installed.

---

## 3. Orchestration: Jenkins vs Argo Workflows

> **Note:** Orchestration is **not implemented in Phase 1**. This section evaluates options for Phase 2 when we need to schedule backfills, connector restarts, schema migrations, and maintenance tasks. The comparison is included here so the team can plan ahead.

**What would be orchestrated:** periodic SOAP ingestion runs (currently one-shot), backfill pipelines for historical data, connector health checks and restarts, database schema migrations, and aggregate table refresh triggers.

### Jenkins pipelines
**PROS (+)**
  * Familiar: Most teams already have Jenkins. Low adoption barrier.
  * Flexible: Can orchestrate any shell command, Java process, Docker operation, or REST API call. Jenkinsfiles are Groovy-based (no Python needed).
  * Plugins: Rich ecosystem — Kafka, Docker, Kubernetes, PostgreSQL, Slack notification plugins all exist.

**CONS (-)**
  * Maintenance burden: Jenkins plugin management is a known pain point — version conflicts, security patches, and plugin deprecations.
  * Statefulness: Jenkins stores all state on the controller node. Requires backups and can become a single point of failure.
  * Modern alternatives: Jenkins is increasingly seen as legacy for cloud-native shops. May conflict with organizational direction.

### Argo Workflows
**PROS (+)**
  * Cloud-native: First-class Kubernetes integration. Workflows are CRDs, steps are containers — natural for our Docker-based stack.
  * DAG support: Visual DAG editor and native support for complex dependency graphs, retries, and conditional branching.
  * Scalability: Each workflow step runs as a pod — scales independently and isolates failures.

**CONS (-)**
  * Kubernetes dependency: Requires a running K8s cluster with RBAC, service accounts, and persistent storage configured. Significant prerequisite if K8s is not already in place.
  * Learning curve: Argo workflow YAML spec is its own DSL. Team needs to learn it.
  * Overkill for PoC: Standing up K8s + Argo for a few cron tasks is disproportionate effort.

**Decision (deferred):** No orchestrator in Phase 1. When Phase 2 requires scheduling, the choice depends on infrastructure direction: if the team adopts Kubernetes, use Argo Workflows. Otherwise, Jenkins is the pragmatic default. A third option worth considering is **simple cron + shell scripts** for the first few scheduled tasks — no framework needed until workflow complexity justifies it.

---

## 4. Serving Database for Aggregates

**Our requirement:** Store two pre-computed aggregate tables (`agg_top_sales_per_city`, `agg_top_salesman_country`) and serve them via a Spring Boot REST API. Write pattern: periodic batch upserts from Kafka Streams. Read pattern: point queries with filters on `period`, `city`, and `limit`.

### PostgreSQL (chosen)
**PROS (+)**
  * Familiar: Universal SQL knowledge. Every team member can query, debug, and administer it.
  * Simplicity: Can serve dual-purpose in a PoC (lineage DB + analytics sink on the same instance). Adding an analytics schema to a dedicated instance is also trivial.
  * Spring Boot integration: Mature JDBC/JPA support. Zero custom driver work for the REST API layer.
  * Sufficient for scale: Aggregate tables have low cardinality — at most `cities × salesmen × periods` rows (thousands, not millions). PostgreSQL handles this effortlessly with proper indexing.
  * Extensions: If analytical query patterns grow, TimescaleDB (PostgreSQL extension) adds time-series optimization and continuous aggregates without changing the database engine.

**CONS (-)**
  * Not columnar: For very large analytical scans (millions of rows, many columns), PostgreSQL's row-based storage is slower than columnar engines.
  * No separation of concerns (if reusing one instance): Using the same PostgreSQL for both the JDBC source and the analytics sink conflates OLTP and OLAP workloads. Production should use separate instances.

### ClickHouse (future option)
**PROS (+)**
  * Columnar performance: 10-100x faster than PostgreSQL for large analytical aggregations and scans over wide tables.
  * Compression: Highly efficient columnar compression — 5-10x less storage than row-based engines for analytical data.
  * MaterializedViews: Built-in support for pre-aggregated materialized views that update on insert — similar to what our Kafka Streams does, but inside the DB.

**CONS (-)**
  * Operational overhead: Separate database to deploy, monitor, backup, and tune. ClickHouse clusters (even single-node) require understanding of MergeTree engines, partitioning, and TTL policies.
  * Spring Boot integration: No official Spring Data module. Requires a JDBC driver (`clickhouse-jdbc`) with manual query building — more work than JPA/JDBC templates for PostgreSQL.
  * Overkill: Our aggregate tables are small pre-computed results. ClickHouse shines when you're scanning raw event tables with billions of rows — not serving pre-aggregated lookup tables.

### Materialized Views in PostgreSQL (considered, not chosen)
**PROS (+)**
  * Simplicity: Write raw events to PostgreSQL, define materialized views with the aggregation SQL, refresh periodically.
  * No Kafka Streams needed: Would eliminate the Streams app entirely for simple aggregations.

**CONS (-)**
  * Requirement violation: The project requires using a modern streaming processor (Kafka Streams/Flink/Spark). Pushing aggregation into the DB would not satisfy that constraint.
  * Refresh latency: `REFRESH MATERIALIZED VIEW` is a full recomputation. Not incremental, and blocks reads during refresh (unless using `CONCURRENTLY`, which requires a unique index and takes longer).

**Decision:** PostgreSQL. The aggregate tables are small, the team knows it, it's already in our stack, and Spring Boot integration is effortless. ClickHouse would only be justified if we later serve raw event data or aggregations over millions of rows directly.

**Risk:** If a single PostgreSQL instance is used for both the ingestion source and the analytics sink, OLTP and OLAP workloads will compete for resources. Plan for separate instances before production.

---

## 5. Lineage: OpenLineage + Marquez vs Custom Tables

**Our requirement:** Data lineage tracking across ingestion sources, Kafka topics, processing jobs, and output tables.

### OpenLineage + Marquez (chosen)
**PROS (+)**
  * Open standard: OpenLineage is a cross-platform lineage spec adopted by Airflow, Spark, dbt, and others. Future-proofs lineage metadata format.
  * Built-in UI: Marquez Web provides dataset/job/run visualization out of the box — no custom frontend needed.
  * API-first: Marquez API accepts lineage events via REST. Kafka Streams apps and Connect jobs can emit events with a small client library.
  * Community momentum: Active LFAI & Data project with growing ecosystem support.

**CONS (-)**
  * Infrastructure cost: Requires 3 additional containers (Marquez API, Marquez Web, Marquez DB). That's non-trivial overhead for a PoC.
  * Instrumentation effort: Pipeline components (Streams app, Connect jobs) must be explicitly instrumented to emit OpenLineage events. This is not automatic — it requires integrating the OpenLineage Java client.
  * Learning curve: Understanding OpenLineage facets (schema, source, SQL), namespaces, and job hierarchies takes time.

### Custom lineage tables in PostgreSQL
**PROS (+)**
  * Minimal infra: One schema in the existing PostgreSQL instance. No new containers.
  * Full control: Custom table design tailored to exactly the metadata we track (source → topic → job → table → endpoint).
  * Simpler for PoC: A `lineage_edges` table with `(source, destination, job_name, timestamp)` is trivial to query and maintain.

**CONS (-)**
  * No UI: You must build your own visualization or use raw SQL queries. Lineage graphs are hard to reason about in tabular form.
  * Non-standard: Custom schema means no interoperability with other tools (dbt, Airflow, Spark) that speak OpenLineage.
  * Maintenance: As the pipeline grows, maintaining a custom lineage model becomes increasingly painful vs adopting a purpose-built tool.

**Decision:** OpenLineage + Marquez. The upfront infrastructure cost is higher, but the long-term benefit (standard format, UI, interoperability with future tools) outweighs a throwaway custom table. Instrumentation of pipeline components (Streams app, Connect jobs) must be planned from day one — not deferred.

**Note:** Pipeline instrumentation is not automatic. Phase 1 should include the OpenLineage client library in both the Streams app and ingestion services so lineage events are emitted from the first run.

---

## 6. Schema Governance: Avro vs JSON Schema vs Protobuf

> **Note:** Schema governance is a **Phase 2** item. All connectors and producers should start with `JsonConverter` to keep PoC setup simple, but Schema Registry enforcement must be planned before production. This section evaluates which serialization format to adopt when that time comes.

### Avro + Schema Registry
**PROS (+)**
  * Ecosystem standard: Default serialization for Confluent Platform. All Kafka Connect connectors support Avro natively via `AvroConverter`.
  * Compact binary format: Smaller message size than JSON. Schema is stored in the registry, not in every message.
  * Schema evolution: Built-in backward/forward/full compatibility checks. Adding a field won't break existing consumers.
  * Code generation: `avro-maven-plugin` generates Java classes from `.avsc` files — type-safe processing in Kafka Streams.

**CONS (-)**
  * Schema definition: Avro's `.avsc` JSON schema format is verbose and unfamiliar to devs who know Protobuf or JSON Schema.
  * Registry dependency: Every producer/consumer call requires a registry lookup (cached). Adds a runtime dependency.

### JSON Schema + Schema Registry
**PROS (+)**
  * Human-readable: JSON payloads are directly inspectable in Kafka UI, logs, and debugging tools.
  * Lower barrier: No code generation step. Schemas are written in familiar JSON Schema syntax.

**CONS (-)**
  * Larger messages: Full JSON text in every message. For high-throughput topics, this wastes bandwidth and storage.
  * Weaker evolution: JSON Schema compatibility checking in Confluent Schema Registry is less mature than Avro's.

### Protobuf + Schema Registry
**PROS (+)**
  * Compact: Binary encoding comparable to Avro. Strong code generation for Java via `protoc`.
  * Multi-language: If the platform later adds non-Java consumers (Go, Rust, C#), Protobuf has excellent cross-language support.

**CONS (-)**
  * Connect support: Some older Kafka Connect connectors have limited Protobuf support. Requires `ProtobufConverter`.
  * Adoption: If the team and tooling are already Avro-oriented, adding Protobuf creates fragmentation.

**Decision (planned for Phase 2):** Avro + Schema Registry. It's the Confluent ecosystem default, all standard connectors support it, and `avro-maven-plugin` code generation fits the Java stack. The migration path is to switch connectors from `JsonConverter` to `AvroConverter` and define `.avsc` schemas for all raw and curated topics.

---

## 7. Observability: Prometheus + Grafana vs ELK

> **Note:** Observability infrastructure is a **Phase 2** item. The PoC runs without monitoring to keep initial setup lean. This section evaluates the target observability stack for when the pipeline is hardened for production.

### Prometheus + Grafana + OpenTelemetry (planned)
**PROS (+)**
  * JVM-native: Spring Boot Actuator can expose Prometheus metrics at `/actuator/prometheus` with minimal configuration. Kafka Streams exposes JMX metrics that Prometheus can scrape via JMX Exporter.
  * Lightweight: Prometheus is a single binary. Grafana provides mature dashboarding with pre-built Kafka and JVM dashboard templates.
  * Tracing: OpenTelemetry Java agent provides distributed tracing across the API, Streams app, and Kafka without code changes.
  * Alerting: Alertmanager integrates with Prometheus for threshold-based alerts (lag, error rate, latency).

**CONS (-)**
  * Additional containers: Prometheus, Grafana, and optionally Alertmanager add 2-3 more services to docker-compose.
  * Configuration: Prometheus scrape configs, Grafana datasources, and dashboard JSON must be maintained.

### ELK (Elasticsearch + Logstash + Kibana)
**PROS (+)**
  * Unified logs + metrics: Can aggregate structured logs, metrics, and APM traces in one platform.
  * Full-text search: Powerful log search and analysis via Kibana.

**CONS (-)**
  * Resource heavy: Elasticsearch requires significant RAM (minimum 2 GB heap). Overkill for PoC-scale observability.
  * Constraint alignment: Logstash pipelines are often configured with Ruby filters or custom scripts. While not Python, the ecosystem is heavier than Prometheus.
  * Operational overhead: ELK cluster management (index lifecycle, shard allocation, upgrades) is non-trivial.

**Decision (planned for Phase 2):** Prometheus + Grafana + OpenTelemetry. Lighter footprint, better fit for JVM/Kafka monitoring, and Spring Boot Actuator integration is trivial. ELK may be added later specifically for centralized log aggregation if needed.

---

## Risk Register

| # | Risk | Likelihood | Impact | Mitigation |
|---|------|-----------|--------|------------|
| 1 | FilePulse connector instability in production | Medium | High | Fall back to Spooldir connector or custom Java file-watcher producer |
| 2 | Duplicate records from JDBC at-least-once delivery | High | Medium | Deduplicate in Kafka Streams by `(id, updated_at)` composite key |
| 3 | No schema validation in Phase 1 (JsonConverter) | High | Medium | Phase 2: switch to AvroConverter + Schema Registry with compatibility policies |
| 4 | Single PostgreSQL instance for OLTP source and OLAP sink | Low (PoC) | Low (PoC) | Plan separate instances before production |
| 5 | Lineage instrumentation not included in Phase 1 | Medium | Low | Include OpenLineage client in Streams app and ingestion services from Phase 1 |
| 6 | Kafka Streams partition-bound scaling | Low | Medium | Increase topic partitions; if insufficient, evaluate Flink SQL migration |
| 7 | Connect plugin versions not pinned | Medium | Medium | Build custom Connect Docker image with pre-installed, version-pinned plugins |

---

## Summary

- **Stream processing:** Kafka Streams — lowest operational cost, natural fit for our two windowed aggregation pipelines, zero cluster management. Flink SQL is the upgrade path if processing complexity grows.
- **Ingestion:** Hybrid Kafka Connect (JDBC + FilePulse) for standard sources + custom Java producer for SOAP. Minimizes custom code while handling the hardest integration (WS-*) with full flexibility.
- **Orchestration:** Deferred to Phase 2. No orchestrator needed yet — evaluate Argo Workflows (if K8s) or Jenkins (if not) when scheduling requirements emerge.
- **Serving DB:** PostgreSQL — already in the stack, team knows it, aggregate tables are small. ClickHouse only if raw event analytics are needed later.
- **Lineage:** OpenLineage + Marquez — plan instrumentation of pipeline components from day one, not as an afterthought.
- **Schema governance (Phase 2):** Avro + Schema Registry. Start with JsonConverter for PoC simplicity; migrate to AvroConverter with `.avsc` schemas for all topics in Phase 2.
- **Observability (Phase 2):** Prometheus + Grafana + OpenTelemetry. Spring Boot Actuator makes Prometheus integration minimal effort.

**Biggest risks to address early:** schema validation gaps on ingestion, potential duplicate records from JDBC at-least-once delivery, and lineage instrumentation being skipped. All should be addressed in the Phase 2 hardening plan.
