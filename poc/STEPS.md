# STEPS

This file contains everything needed to set up and run the PoC scaffold.

## 1) Setup

1. Install prerequisites:
   - Docker Desktop (with Compose v2)
   - JDK 21
   - Maven 3.9+
   - Optional: `curl`, `psql`
2. Go to the PoC folder:
   ```powershell
   cd c:\Users\gamba\Documents\github\data-kata\poc
   ```
3. Create local environment file:
   ```powershell
   Copy-Item .env.example .env
   ```
4. Start infrastructure:
   ```powershell
   docker compose up -d
   ```
5. Verify services:
   - Kafka UI: `http://localhost:8080`
   - Kafka Connect: `http://localhost:8083/connectors`
   - Schema Registry: `http://localhost:8081/subjects`
   - Marquez UI: `http://localhost:3000`
   - Postgres: `localhost:5432` user/password `analytics/analytics`

## 2) Running

### A. Start API service
1. Open a terminal in `poc/api`.
2. Run:
   ```powershell
   mvn spring-boot:run
   ```
3. Check endpoints:
   - `http://localhost:8088/v1/analytics/top-sales-per-city?period=monthly&limit=10`
   - `http://localhost:8088/v1/analytics/top-salesman-country?period=monthly`

### B. Run streams app
1. Open a terminal in `poc/streams`.
2. Package and run:
   ```powershell
   mvn -q -DskipTests package
   java -jar target/streams-0.1.0-SNAPSHOT.jar
   ```

### C. Run SOAP ingestion placeholder
1. Open a terminal in `poc/ingestion-soap`.
2. Run:
   ```powershell
   mvn -q -DskipTests exec:java -Dexec.mainClass=com.example.ingestion.SoapIngestionApplication
   ```

## 3) Optional: Create topics explicitly

```powershell
docker exec -it poc-kafka kafka-topics.sh --bootstrap-server kafka:29092 --create --if-not-exists --topic curated.sales_events --partitions 3 --replication-factor 1
docker exec -it poc-kafka kafka-topics.sh --bootstrap-server kafka:29092 --create --if-not-exists --topic agg.top_sales_per_city --partitions 3 --replication-factor 1
docker exec -it poc-kafka kafka-topics.sh --bootstrap-server kafka:29092 --create --if-not-exists --topic agg.top_salesman_country --partitions 3 --replication-factor 1
```

## 4) Optional: Register connectors

Use templates in `poc/connectors/`.

```powershell
curl -X POST http://localhost:8083/connectors -H "Content-Type: application/json" --data-binary "@connectors/jdbc-source-sales.template.json"
```

Note: install connector plugins first (JDBC and FilePulse) for successful registration.

## 5) Shutdown

```powershell
docker compose down
```

To remove volumes too:

```powershell
docker compose down -v
```

## 6) Troubleshooting

- If `marquez-api` fails with `password authentication failed`, reset local volumes and restart:
   ```powershell
   docker compose down -v
   docker compose up -d
   ```
