# Trade-offs and Decision Notes

This PoC chooses a Kafka Streams centered architecture. The table below compares realistic alternatives before committing.

## 1) Stream processing engine

| Option | Pros | Cons | Decision |
|---|---|---|---|
| Kafka Streams | Native Kafka integration, simple deployment (just a JVM app), good for event-time windows and joins, low operational overhead for PoC | Tied to Kafka, fewer built-in connectors than Flink ecosystem, less expressive for very advanced stateful patterns | Chosen for PoC |
| Apache Flink | Strong state management, rich windowing and event-time semantics, mature SQL/table API | Higher operational complexity, typically needs dedicated cluster/runtime | Consider for scale-up |
| Spark Structured Streaming | Unified batch + stream, good if Spark already exists | Micro-batch model may add latency, heavier runtime footprint | Not chosen for MVP |

## 2) Ingestion approach

| Option | Pros | Cons | Decision |
|---|---|---|---|
| Kafka Connect + custom SOAP adapter | Fast for DB/files using standard connectors, reusable operational model, SOAP path stays in Java/Kotlin | Connector plugin management can be tricky, SOAP still requires custom coding | Chosen |
| Fully custom ingestion microservices | Maximum control and custom logic | More code, more maintenance, slower initial delivery | Only for special sources |

## 3) Orchestration (non-Python constraint)

| Option | Pros | Cons | Decision |
|---|---|---|---|
| Jenkins pipelines | Familiar for many teams, broad plugin support | Plugin sprawl and governance overhead | Good default |
| Argo Workflows | Cloud-native DAG workflows, Kubernetes-first | Requires Kubernetes maturity | Use if K8s is standard |

## 4) Serving database for aggregates

| Option | Pros | Cons | Decision |
|---|---|---|---|
| PostgreSQL analytics schema | Simple, reliable, SQL-friendly, low barrier for API integration | Limited for very large OLAP workloads vs columnar MPP stores | Chosen for PoC |
| ClickHouse | Very fast analytics queries | Additional infra and operational learning | Future option |

## 5) Lineage stack

| Option | Pros | Cons | Decision |
|---|---|---|---|
| OpenLineage + Marquez | Open standard, good visibility for runs and datasets | Extra components to operate | Chosen |
| Custom lineage tables | Minimal infra | Reinvents standard lineage model and UI | Not chosen |

## Summary
- The selected stack optimizes for fast delivery, Java/Kotlin alignment, and low-to-medium operational complexity.
- The biggest known trade-off is connector plugin lifecycle management in Kafka Connect.
- If this PoC proves value and scale needs grow, Flink + stronger data contract enforcement should be the first upgrade path.
