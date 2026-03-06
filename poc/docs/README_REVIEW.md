# README Review Notes

## What was missing
- No explicit local prerequisites section (Docker, JDK, Maven versions).
- No runnable `docker-compose.yml` in the folder.
- No environment variable template (`.env.example`).
- No bootstrap SQL/init for analytics tables.
- No concrete setup/run commands for contributors.
- No decision trade-off comparison document before implementation.
- No glossary for non-obvious technical terms.

## What was added
- `poc/docker-compose.yml` for Kafka, Connect, Schema Registry, Kafka UI, Postgres, Marquez.
- `poc/.env.example` with common ports.
- `poc/initdb/01_analytics_schema.sql` for aggregate tables.
- `poc/TRADE_OFFS.md` with alternatives and rationale.
- `poc/GLOSSARY.md` for technical terms.
- `poc/STEPS.md` with setup and running instructions.
- Java module scaffolds under `poc/api`, `poc/streams`, and `poc/ingestion-soap`.

## Potential corrections to keep in mind
- README states "no Python" and suggests Argo/Jenkins; this is consistent.
- Connector choice is valid, but plugin installation method should be documented clearly when moving beyond PoC.
- SOAP ingestion details should include WSDL contract and mapping rules in the next iteration.
