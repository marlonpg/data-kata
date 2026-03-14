# Data Pipeline POC v2

## Requirements

Must create a modern data pipeline with:
1. Ingestion for 3 different data sources (Relational DB, File system and Traditional WS-*)
2. Modern processing with Spark, Flink or Kafka Streams
3. Data Lineage
4. Observability
5. Pipelines must have at least 2 pipelines:
    1. Top Sales per City
    2. Top Salesman in the whole country
6. The final Aggregated results must be in a dedicated DB and API
7. Restrictions
    1. Python
    2. Red-Shift
    3. Hadoop

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│  STEP 1 - DATA PROCESSING                                          │
│                                                                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                          │
│  │   FILE   │  │ DATABASE │  │    WS    │   Data Sources            │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘                          │
│       │              │             │                                │
│       ▼              ▼             ▼         ┌─────────────────┐    │
│    ┌──────────────────────────────────┐      │  KAFKA CONNECT  │    │
│    │        KAFKA (STREAMS)           │◄────►│  KAFKA CONNECTOR│    │
│    └──────────────┬───────────────────┘      │  KAFKA UI       │    │
│                   │                          └─────────────────┘    │
│                   ▼                                                 │
│           ┌──────────────┐                                          │
│           │   DATABASE   │  Target PostgreSQL                       │
│           └──────┬───────┘                                          │
│                  │                                                  │
│                  ▼                                                  │
│           ┌──────────────┐                                          │
│           │   MARQUEZ    │  STEP 3 - DATA LINEAGE                   │
│           └──────────────┘                                          │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────┐
│ STEP 2 -        │
│ MONITORING      │
│                 │
│ ┌─────────────┐ │
│ │ PROMETHEUS  │ │
│ ├─────────────┤ │
│ │  GRAFANA    │ │
│ ├─────────────┤ │
│ │    LOKI     │ │
│ └─────────────┘ │
└─────────────────┘
```

## Tech Stack

| Component | Technology |
|---|---|
| Source DB | PostgreSQL 16 |
| File Simulator | Spring Boot + Kafka Producer |
| WS Simulator | Spring Boot + Kafka Producer |
| Message Broker | Apache Kafka (Confluent 7.6.0, KRaft mode) |
| CDC | Kafka Connect + JDBC Source Connector |
| Stream Processing | Kafka Streams (Spring Kafka) |
| Target DB | PostgreSQL 16 |
| API | Spring Boot + Spring Data JPA |
| Data Lineage | Marquez (OpenLineage) |
| Metrics | Prometheus |
| Dashboards | Grafana |
| Log Aggregation | Loki + Promtail |
| Kafka Monitoring | Kafka UI |

## Services & Ports

| Service | Port | URL |
|---|---|---|
| Kafka | 9092 | `localhost:9092` |
| Kafka Connect | 8083 | `http://localhost:8083` |
| Kafka UI | 8080 | `http://localhost:8080` |
| Source PostgreSQL | 5432 | `localhost:5432` |
| Target PostgreSQL | 5433 | `localhost:5433` |
| Kafka Streams | 8081 | `http://localhost:8081/actuator/health` |
| Analytics API | 8082 | `http://localhost:8082/api/top-sales-per-city` |
| Marquez API | 5000 | `http://localhost:5000` |
| Marquez Web UI | 3000 | `http://localhost:3000` |
| Prometheus | 9090 | `http://localhost:9090` |
| Grafana | 3001 | `http://localhost:3001` (admin/admin) |
| Loki | 3100 | `http://localhost:3100` |

## How to Run

```bash
cd poc-v2
docker compose up --build -d
```

## API Endpoints

- `GET http://localhost:8082/api/top-sales-per-city` — Top Sales per City (sorted by total amount)
- `GET http://localhost:8082/api/top-salesman` — Top Salesman in the Country (sorted by total amount)

## Pipelines

### Pipeline 1: Top Sales per City
- **Inputs:** `sales-db`, `sales-file`, `sales-ws` Kafka topics
- **Processing:** Kafka Streams — merge 3 sources → group by city → aggregate (total amount, sale count)
- **Output:** `top_sales_per_city` table in Target PostgreSQL

### Pipeline 2: Top Salesman in Country
- **Inputs:** `sales-db`, `sales-file`, `sales-ws` Kafka topics
- **Processing:** Kafka Streams — merge 3 sources → group by salesman → aggregate (total amount, sale count)
- **Output:** `top_salesman_country` table in Target PostgreSQL