# PoC Proposal: Modern Data Pipeline (Kafka Streams-based, no Python/Redshift/Hadoop)

This PoC is updated to reflect your clarification: **do not use Python, Redshift, or Hadoop**.

## 1) High-level architecture (recommended)

- **Ingestion layer (3 source types)**
  - **Relational DB source**: PostgreSQL/MySQL -> Kafka via **Kafka Connect JDBC Source** (CDC/incremental mode).
  - **File system source**: CSV/JSON/Parquet from shared object storage or NFS -> Kafka via **Kafka Connect FilePulse**.
  - **Traditional WS-* source (SOAP)**: Java/Kotlin ingestion service (JAX-WS or Apache CXF) -> Kafka producer.

- **Streaming/processing layer**
  - **Kafka + Kafka Streams** for transformations, joins, windowed aggregations, and ranking.
  - Use two Kafka Streams applications (or one app with two topologies): one per required pipeline.

- **Storage & serving layer**
  - Final aggregated outputs persisted in **PostgreSQL** (dedicated analytics database).
  - **Spring Boot API (Java/Kotlin)** exposes endpoints for the two required analytics results.

- **Orchestration**
  - **Apache Airflow alternative (non-Python)**: use **Argo Workflows** or **Jenkins pipelines** for scheduling ingestion and maintenance tasks.

- **Lineage**
  - **OpenLineage + Marquez** integrated with ingestion jobs and stream deployment metadata.
  - Naming convention links source systems -> Kafka topics -> aggregates -> DB tables -> API endpoints.

- **Observability**
  - Metrics: Prometheus + Grafana.
  - Logs: OpenSearch/ELK.
  - Tracing: OpenTelemetry.
  - Alerts: Alertmanager + on-call routing.

---

## 2) Pipeline design for required use cases

### Pipeline A: Top Sales per City

1. Ingest events from DB, files, and SOAP into canonical topic `curated.sales_events`.
2. Kafka Streams processing:
   - Normalize schema and amounts.
   - Aggregate sales by `(city_id, salesman_id)` for a given period.
   - Rank per city and produce `agg.top_sales_per_city`.
3. Sink to PostgreSQL table `analytics.agg_top_sales_per_city`.
4. Serve through API endpoint:
   - `GET /v1/analytics/top-sales-per-city?period=monthly&limit=10`

### Pipeline B: Top Salesman in the Whole Country

1. Consume `curated.sales_events`.
2. Kafka Streams processing:
   - Aggregate by `salesman_id` nationally.
   - Rank descending and keep top N (or top 1).
   - Produce `agg.top_salesman_country`.
3. Sink to PostgreSQL table `analytics.agg_top_salesman_country`.
4. Serve through API endpoint:
   - `GET /v1/analytics/top-salesman-country?period=monthly`

---

## 3) Technology choices (respecting restrictions)

- **Processing**: Kafka Streams (as selected).
- **Ingestion**: Kafka Connect (JDBC, FilePulse) + Java/Kotlin SOAP adapter.
- **Final DB (dedicated)**: PostgreSQL.
- **API**: Spring Boot (Java/Kotlin).
- **Lineage**: OpenLineage + Marquez.
- **Observability**: Prometheus, Grafana, OpenTelemetry, OpenSearch.

> Explicitly excluded: **Python, Redshift, Hadoop**.

---

## 4) Topic and table blueprint

### Kafka topics
- `raw.db.sales`
- `raw.files.sales`
- `raw.soap.sales`
- `curated.sales_events`
- `agg.top_sales_per_city`
- `agg.top_salesman_country`

### PostgreSQL schema (example)
- `analytics.agg_top_sales_per_city`
  - `period_start`, `period_end`, `city_id`, `city_name`, `salesman_id`, `salesman_name`, `total_sales`, `rank_in_city`, `updated_at`
- `analytics.agg_top_salesman_country`
  - `period_start`, `period_end`, `salesman_id`, `salesman_name`, `total_sales`, `national_rank`, `updated_at`

---

## 5) Delivery plan (phased)

### Phase 1 (MVP)
- Stand up Kafka, Kafka Connect, PostgreSQL, API service, and Marquez.
- Build 3 ingestion paths into Kafka.
- Implement both Kafka Streams pipelines.
- Persist final aggregates into PostgreSQL.
- Expose two API endpoints.

### Phase 2 (hardening)
- Add Schema Registry and compatibility policies.
- Add replay strategy from immutable file storage.
- Add SLOs, alerts, and operational dashboards.
- Add authentication/authorization and secrets management.

---

## 6) Non-functional best practices

- Use Avro/Protobuf + Schema Registry for contracts.
- Configure idempotent producers and EOS where practical.
- Define partitioning strategy for balanced throughput.
- Add dead-letter topics and retry policies.
- Implement backfill and deterministic reprocessing runbooks.

---

## 7) Starter API contract

- `GET /v1/analytics/top-sales-per-city`
  - Query params: `period`, `start_date`, `end_date`, `limit`
  - Returns: ranked salesmen per city.

- `GET /v1/analytics/top-salesman-country`
  - Query params: `period`, `start_date`, `end_date`
  - Returns: ranked salesmen nationwide.

---

## 8) Next implementation step

Create a runnable scaffold under `/poc` with:
- `docker-compose.yml` (Kafka, Connect, PostgreSQL, Marquez, API)
- `ingestion-soap/` (Java/Kotlin WS-* adapter)
- `streams/` (Kafka Streams project)
- `api/` (Spring Boot service)
- `docs/` (runbook, lineage map, dashboards)

This gives you a practical baseline without violating the technology restrictions.
