package com.example.wssimulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@SpringBootApplication
public class WsSimulatorApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(WsSimulatorApplication.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Random random = new Random();

    private static final String TOPIC = "sales-ws";
    private static final String[] CITIES = {"São Paulo", "Rio de Janeiro", "Belo Horizonte", "Curitiba", "Salvador", "Porto Alegre"};
    private static final String[] SALESMEN = {"Carlos Silva", "Ana Santos", "Pedro Oliveira", "Maria Costa", "João Ferreira"};
    private static final String[] DESCRIPTIONS = {"SOAP Order Processing", "WS Inventory Sync", "Legacy CRM Export", "EDI Transaction", "XML Batch Import"};

    public WsSimulatorApplication(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public static void main(String[] args) {
        SpringApplication.run(WsSimulatorApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        long saleId = 20000;
        log.info("WS Simulator started. Producing events to topic: {}", TOPIC);

        while (!Thread.currentThread().isInterrupted()) {
            Map<String, Object> sale = new HashMap<>();
            sale.put("sale_id", saleId);
            sale.put("amount", Math.round(random.nextDouble() * 500 * 100.0) / 100.0);
            sale.put("description", DESCRIPTIONS[random.nextInt(DESCRIPTIONS.length)]);
            sale.put("city", CITIES[random.nextInt(CITIES.length)]);
            sale.put("country", "Brazil");
            sale.put("salesman", SALESMEN[random.nextInt(SALESMEN.length)]);
            sale.put("source", "ws");
            sale.put("created_date", System.currentTimeMillis());

            String json = mapper.writeValueAsString(sale);
            kafkaTemplate.send(TOPIC, String.valueOf(saleId), json);
            log.info("Produced sale event: id={}, city={}, salesman={}", saleId, sale.get("city"), sale.get("salesman"));

            saleId++;
            Thread.sleep(5000 + random.nextInt(5000));
        }
    }
}
