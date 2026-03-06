# Connector Templates

These are starter payloads for Kafka Connect REST API.

## Important
- `jdbc-source-sales.template.json` requires JDBC connector plugin.
- `filepulse-sales.template.json` requires FilePulse plugin.
- The base `cp-kafka-connect` image does not include both plugins by default.

## Register connector
Use Kafka Connect REST once the stack is up:

```bash
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @connectors/jdbc-source-sales.template.json
```

Replace with the FilePulse template as needed.
