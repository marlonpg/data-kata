# Trade-offs and Decision Notes

This section is written like a side-by-side pros/cons comparison, similar to the “AUTH0 vs ORY” example you provided.

---

## Kafka Streams vs Apache Flink vs Spark Structured Streaming

### Kafka Streams
**PROS (+)**
  * Integration: Native Kafka integration, no separate cluster needed.
  * Deployment: Simple JVM app deployment (no cluster orchestration).
  * Operations: Low operational overhead for PoC workloads.
  * APIs: Rich DSL for event-time windows, joins, and stateful patterns.

**CONS (-)**
  * Lock-in: Tightly tied to Kafka; not framework-agnostic.
  * Ecosystem: Fewer built-in connectors and integrations vs Flink.
  * Expressiveness: Complex stateful patterns can be harder than Flink.

### Apache Flink
**PROS (+)**
  * State: Strong state management and fault-tolerance.
  * Semantics: Mature event-time and windowing semantics.
  * APIs: SQL/table API and support for batch/stream unified processing.

**CONS (-)**
  * Complexity: Requires a dedicated Flink cluster (more operational work).
  * Footprint: Heavier runtime and resource requirements.

### Spark Structured Streaming
**PROS (+)**
  * Unified: One platform for batch + streaming.
  * Adoption: Good if Spark already exists in the environment.

**CONS (-)**
  * Latency: Micro-batch model adds latency vs true streaming.
  * Runtime: Higher resource/cluster overhead for streaming.

---

## Ingestion: Kafka Connect + SOAP adapter vs Fully custom services

### Kafka Connect + custom SOAP adapter
**PROS (+)**
  * Standard: Leverages existing connectors (JDBC, FilePulse).
  * Reusable: Connector configs can be reused across environments.
  * Alignment: Keeps ingestion in Java/Kotlin for SOAP.

**CONS (-)**
  * Plugins: Must manage connector plugin installation and versions.
  * Debugging: Connect errors can be opaque.
  * Flexibility: Limited for deeply custom transformations inline.

### Fully custom ingestion microservices
**PROS (+)**
  * Control: Full freedom for schema mapping, error handling.
  * Custom logic: Easy to embed non-standard ingestion flows.

**CONS (-)**
  * Dev effort: More code, tests, and maintenance.
  * Ops: More services to deploy and monitor.

---

## Orchestration (non-Python) comparison

### Jenkins pipelines
**PROS (+)**
  * Familiar: Widely used, lots of plugins.
  * Flexible: Can orchestrate any shell/Java workflow.

**CONS (-)**
  * Maintenance: Plugin management can become heavy.
  * UI: Pipeline editing/visualization is less modern.

### Argo Workflows
**PROS (+)**
  * Cloud-native: Designed for Kubernetes and DAGs.
  * Scalability: Good for large, distributed workflows.

**CONS (-)**
  * Kubernetes requirement: Needs K8s maturity.
  * Ops: Requires managing a K8s control plane.

---

## Serving database for aggregates

### PostgreSQL (chosen)
**PROS (+)**
  * Familiar: Easy SQL access and tooling.
  * Simplicity: Low barrier for PoC and small analytics.

**CONS (-)**
  * Scale: Not ideal for very large, analytic-only workloads.

### ClickHouse (future option)
**PROS (+)**
  * Performance: Extremely fast analytical queries.
  * Compression: Efficient storage for large datasets.

**CONS (-)**
  * Ops: Requires learning a new database and tuning.
  * Integration: Less straightforward for OLTP-style APIs.

---

## Lineage stack: OpenLineage + Marquez vs Custom tables

### OpenLineage + Marquez
**PROS (+)**
  * Open standard with broad tool support.
  * Built-in UI for datasets, jobs, and runs.

**CONS (-)**
  * Infra: Adds extra components to operate.
  * Integration: Requires instrumenting pipelines.

### Custom lineage tables
**PROS (+)**
  * Minimal infra: One DB schema under your control.
  * Simplicity: No extra service to deploy.

**CONS (-)**
  * Reinventing: You rebuild lineage UI/queries.
  * Visibility: Limited compared to purpose-built lineage UIs.

---

## Summary
- This PoC prioritizes fast delivery, low operational overhead, and staying within the constraints (Java/Kotlin + Kafka). 
- The biggest trade-off is managing connector plugins and keeping Kafka Connect stable.
- If this proves valuable, the next improvements would be: 1) stronger schema governance (Schema Registry + compatibility), and 2) evaluating Flink for more advanced stream state/processing needs.
