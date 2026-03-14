package com.example.streams.topology;

import com.example.streams.model.CityAggregation;
import com.example.streams.model.SaleEvent;
import com.example.streams.model.SalesmanAggregation;
import com.example.streams.serde.JsonSerde;
import com.example.streams.sink.PostgresSink;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SalesTopology {

    private static final Logger log = LoggerFactory.getLogger(SalesTopology.class);

    private final PostgresSink postgresSink;

    public SalesTopology(PostgresSink postgresSink) {
        this.postgresSink = postgresSink;
    }

    @Autowired
    public void buildPipeline(StreamsBuilder builder) {
        JsonSerde<SaleEvent> saleEventSerde = new JsonSerde<>(SaleEvent.class);
        JsonSerde<CityAggregation> cityAggSerde = new JsonSerde<>(CityAggregation.class);
        JsonSerde<SalesmanAggregation> salesmanAggSerde = new JsonSerde<>(SalesmanAggregation.class);

        Consumed<String, SaleEvent> consumed = Consumed.with(Serdes.String(), saleEventSerde);

        // Read from 3 source topics
        KStream<String, SaleEvent> salesDb = builder.stream("sales-db", consumed);
        KStream<String, SaleEvent> salesFile = builder.stream("sales-file", consumed);
        KStream<String, SaleEvent> salesWs = builder.stream("sales-ws", consumed);

        // Merge all sources into a single stream
        KStream<String, SaleEvent> allSales = salesDb.merge(salesFile).merge(salesWs);

        allSales.peek((key, value) -> log.info("Processing sale: id={}, city={}, salesman={}, source={}",
                value.getSaleId(), value.getCity(), value.getSalesman(), value.getSource()));

        // ================================================
        // Pipeline 1: Top Sales per City
        // ================================================
        KTable<String, CityAggregation> topSalesPerCity = allSales
                .groupBy(
                        (key, value) -> value.getCity(),
                        Grouped.with(Serdes.String(), saleEventSerde)
                )
                .aggregate(
                        CityAggregation::new,
                        (city, sale, agg) -> {
                            agg.setCity(city);
                            agg.setTotalAmount(agg.getTotalAmount() + sale.getAmount());
                            agg.setSaleCount(agg.getSaleCount() + 1);
                            return agg;
                        },
                        Materialized.with(Serdes.String(), cityAggSerde)
                );

        topSalesPerCity.toStream()
                .peek((city, agg) -> {
                    postgresSink.upsertCityAggregation(agg);
                    log.info("Pipeline 1 - City: {}, Total: {}, Count: {}", city, agg.getTotalAmount(), agg.getSaleCount());
                })
                .to("top-sales-per-city", Produced.with(Serdes.String(), cityAggSerde));

        // ================================================
        // Pipeline 2: Top Salesman in the Country
        // ================================================
        KTable<String, SalesmanAggregation> topSalesman = allSales
                .groupBy(
                        (key, value) -> value.getSalesman(),
                        Grouped.with(Serdes.String(), saleEventSerde)
                )
                .aggregate(
                        SalesmanAggregation::new,
                        (salesman, sale, agg) -> {
                            agg.setSalesman(salesman);
                            agg.setTotalAmount(agg.getTotalAmount() + sale.getAmount());
                            agg.setSaleCount(agg.getSaleCount() + 1);
                            return agg;
                        },
                        Materialized.with(Serdes.String(), salesmanAggSerde)
                );

        topSalesman.toStream()
                .peek((salesman, agg) -> {
                    postgresSink.upsertSalesmanAggregation(agg);
                    log.info("Pipeline 2 - Salesman: {}, Total: {}, Count: {}", salesman, agg.getTotalAmount(), agg.getSaleCount());
                })
                .to("top-salesman-country", Produced.with(Serdes.String(), salesmanAggSerde));

        log.info("Sales topology built: 2 pipelines (Top Sales per City + Top Salesman Country)");
    }
}
