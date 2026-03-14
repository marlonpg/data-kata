package com.example.streams.lineage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.*;

@Component
public class LineageEmitter {

    private static final Logger log = LoggerFactory.getLogger(LineageEmitter.class);

    private final String openlineageUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final Map<String, String> runIds = new HashMap<>();

    public LineageEmitter(@Value("${openlineage.url:http://marquez:5000}") String openlineageUrl) {
        this.openlineageUrl = openlineageUrl;
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
        this.runIds.put("top-sales-per-city", UUID.randomUUID().toString());
        this.runIds.put("top-salesman-country", UUID.randomUUID().toString());
    }

    @PostConstruct
    public void init() {
        Thread emitter = new Thread(() -> {
            try {
                Thread.sleep(15000);
                emitEvent("top-sales-per-city", "START", "top_sales_per_city");
                emitEvent("top-salesman-country", "START", "top_salesman_country");
                log.info("OpenLineage START events emitted for both pipelines");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        emitter.setDaemon(true);
        emitter.start();
    }

    @Scheduled(fixedRate = 60000, initialDelay = 30000)
    public void emitPeriodicLineage() {
        emitEvent("top-sales-per-city", "RUNNING", "top_sales_per_city");
        emitEvent("top-salesman-country", "RUNNING", "top_salesman_country");
    }

    @PreDestroy
    public void shutdown() {
        emitEvent("top-sales-per-city", "COMPLETE", "top_sales_per_city");
        emitEvent("top-salesman-country", "COMPLETE", "top_salesman_country");
        log.info("OpenLineage COMPLETE events emitted for both pipelines");
    }

    private void emitEvent(String jobName, String eventType, String outputTable) {
        try {
            Map<String, Object> event = buildEvent(jobName, eventType, outputTable);
            String json = mapper.writeValueAsString(event);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(openlineageUrl + "/api/v1/lineage"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Lineage {} event for [{}] sent. Status: {}", eventType, jobName, response.statusCode());
        } catch (Exception e) {
            log.warn("Failed to emit lineage event for {}: {}", jobName, e.getMessage());
        }
    }

    private Map<String, Object> buildEvent(String jobName, String eventType, String outputTable) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", eventType);
        event.put("eventTime", Instant.now().toString());
        event.put("producer", "https://github.com/data-kata/kafka-streams-processor");
        event.put("schemaURL", "https://openlineage.io/spec/1-0-5/OpenLineage.json#/definitions/RunEvent");

        Map<String, Object> run = new LinkedHashMap<>();
        run.put("runId", runIds.get(jobName));
        event.put("run", run);

        Map<String, Object> job = new LinkedHashMap<>();
        job.put("namespace", "data-pipeline");
        job.put("name", jobName);
        event.put("job", job);

        // Input datasets - 3 Kafka topics
        List<Map<String, Object>> inputs = new ArrayList<>();
        for (String topic : List.of("sales-db", "sales-file", "sales-ws")) {
            Map<String, Object> input = new LinkedHashMap<>();
            input.put("namespace", "data-pipeline");
            input.put("name", "kafka." + topic);
            input.put("facets", buildInputSchemaFacet());
            inputs.add(input);
        }
        event.put("inputs", inputs);

        // Output dataset - PostgreSQL table
        List<Map<String, Object>> outputs = new ArrayList<>();
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("namespace", "data-pipeline");
        output.put("name", "postgresql.analyticsdb." + outputTable);
        output.put("facets", buildOutputSchemaFacet(outputTable));
        outputs.add(output);
        event.put("outputs", outputs);

        return event;
    }

    private Map<String, Object> buildInputSchemaFacet() {
        Map<String, Object> facets = new LinkedHashMap<>();
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("_producer", "https://github.com/data-kata/kafka-streams-processor");
        schema.put("_schemaURL", "https://openlineage.io/spec/facets/1-0-0/SchemaDatasetFacet.json");
        schema.put("fields", List.of(
                Map.of("name", "sale_id", "type", "BIGINT"),
                Map.of("name", "amount", "type", "DOUBLE"),
                Map.of("name", "description", "type", "VARCHAR"),
                Map.of("name", "city", "type", "VARCHAR"),
                Map.of("name", "country", "type", "VARCHAR"),
                Map.of("name", "salesman", "type", "VARCHAR"),
                Map.of("name", "source", "type", "VARCHAR"),
                Map.of("name", "created_date", "type", "BIGINT")
        ));
        facets.put("schema", schema);
        return facets;
    }

    private Map<String, Object> buildOutputSchemaFacet(String outputTable) {
        Map<String, Object> facets = new LinkedHashMap<>();
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("_producer", "https://github.com/data-kata/kafka-streams-processor");
        schema.put("_schemaURL", "https://openlineage.io/spec/facets/1-0-0/SchemaDatasetFacet.json");

        if ("top_sales_per_city".equals(outputTable)) {
            schema.put("fields", List.of(
                    Map.of("name", "city", "type", "VARCHAR"),
                    Map.of("name", "total_amount", "type", "DECIMAL"),
                    Map.of("name", "sale_count", "type", "BIGINT"),
                    Map.of("name", "updated_at", "type", "TIMESTAMP")
            ));
        } else {
            schema.put("fields", List.of(
                    Map.of("name", "salesman", "type", "VARCHAR"),
                    Map.of("name", "total_amount", "type", "DECIMAL"),
                    Map.of("name", "sale_count", "type", "BIGINT"),
                    Map.of("name", "updated_at", "type", "TIMESTAMP")
            ));
        }

        facets.put("schema", schema);
        return facets;
    }
}
